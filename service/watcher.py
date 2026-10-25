"""RepoCast PC service: polls GitHub issues for podcast requests and builds them.

The app creates an issue labelled `repocast-request` whose body is JSON:
    {"url": "https://github.com/owner/repo", "lang": "de" | "en", "mode": "overview" | "learn"}
For each one this script clones the repo, lets a sandboxed headless Claude write the
episode scripts, turns them into MP3s and publishes them to the gh-pages worktree.
"""
import json
import logging
import re
import shutil
import subprocess
import sys
import time
from datetime import date
from logging.handlers import RotatingFileHandler
from pathlib import Path

import generate

QUEUE_REPO = "KamilKirstein90/repocast"
ALLOWED_AUTHOR = "KamilKirstein90"
PAGES_URL = "https://kamilkirstein90.github.io/repocast/"
DAILY_LIMIT = 3
POLL_SECONDS = 60
CLAUDE_TIMEOUT = 45 * 60

ROOT = Path(__file__).resolve().parent.parent
SERVICE = ROOT / "service"
WORK = ROOT / "work"
SITE = ROOT / "site"
STATE_FILE = SERVICE / "state.json"

URL_RE = re.compile(r"^https://github\.com/([\w.-]+)/([\w.-]+?)(?:\.git)?/?$")
LABELS = {
    "repocast-request": "1d76db",
    "processing": "fbca04",
    "done": "0e8a16",
    "failed": "d73a4a",
}
# Sandbox for the headless Claude run: file tools only, confined to the job folder.
CLAUDE_ARGS = [
    "-p", "--restricted", "--strict-mcp-config",
    "--tools", "Read,Glob,Grep,Write",
    "--permission-mode", "acceptEdits", "--permission-prompts", "none",
    "--max-turns", "80", "--output-format", "json",
]

log = logging.getLogger("repocast")


class JobError(Exception):
    pass


def run(cmd: list[str], cwd: Path | None = None, timeout: int = 600, stdin: str | None = None) -> str:
    result = subprocess.run(cmd, cwd=cwd, input=stdin, capture_output=True, text=True,
                            encoding="utf-8", errors="replace", timeout=timeout)
    if result.returncode != 0:
        raise JobError(f"{cmd[0]} failed ({result.returncode}): {(result.stderr or result.stdout).strip()[-500:]}")
    return result.stdout


def gh(*args: str) -> str:
    return run(["gh", *args, "--repo", QUEUE_REPO])


def load_state() -> dict:
    state = json.loads(STATE_FILE.read_text(encoding="utf-8")) if STATE_FILE.exists() else {}
    if state.get("date") != date.today().isoformat():
        state = {"date": date.today().isoformat(), "jobs": 0}
    return state


def save_state(state: dict) -> None:
    STATE_FILE.write_text(json.dumps(state), encoding="utf-8")


def parse_request(issue: dict) -> dict:
    """Treat the issue body strictly as data and validate every field."""
    body = issue["body"].strip()
    match = re.search(r"\{.*\}", body, re.S)
    if not match:
        raise JobError("Issue body contains no JSON request.")
    try:
        req = json.loads(match.group(0))
    except json.JSONDecodeError as e:
        raise JobError(f"Invalid JSON: {e}") from None
    url_match = URL_RE.match(str(req.get("url", "")).strip())
    if not url_match:
        raise JobError("URL must look like https://github.com/owner/repo")
    lang = req.get("lang", "de")
    mode = req.get("mode", "overview")
    if lang not in ("de", "en") or mode not in ("overview", "learn"):
        raise JobError("lang must be de|en and mode overview|learn")
    owner, name = url_match.groups()
    return {"url": f"https://github.com/{owner}/{name}", "owner": owner, "name": name, "lang": lang, "mode": mode}


def build_podcast(req: dict) -> tuple[dict, dict]:
    job_dir = WORK / f"{req['owner']}__{req['name']}"
    if job_dir.exists():
        shutil.rmtree(job_dir, onerror=lambda f, p, _: (Path(p).chmod(0o666), f(p)))
    (job_dir / "out" / "scripts").mkdir(parents=True)

    log.info("Cloning %s", req["url"])
    run(["git", "clone", "--depth", "1", "--quiet", req["url"], str(job_dir / "repo")], timeout=600)
    sha = run(["git", "-C", str(job_dir / "repo"), "rev-parse", "HEAD"]).strip()
    job = {"url": req["url"], "lang": req["lang"], "mode": req["mode"], "sha": sha}
    (job_dir / "job.json").write_text(json.dumps(job), encoding="utf-8")

    log.info("Writing scripts with Claude (%s, %s)", req["lang"], req["mode"])
    claude = shutil.which("claude") or "claude"
    prompt = (SERVICE / "PROMPT.md").read_text(encoding="utf-8")
    output = run([claude, *CLAUDE_ARGS], cwd=job_dir, timeout=CLAUDE_TIMEOUT, stdin=prompt)
    usage = {}
    try:
        result = json.loads(output)
        usage = {"turns": result.get("num_turns"), "usage": result.get("usage"),
                 "duration_s": round(result.get("duration_ms", 0) / 1000)}
        if result.get("is_error"):
            raise JobError(f"Claude reported an error: {str(result.get('result'))[:300]}")
    except json.JSONDecodeError:
        log.warning("Could not parse Claude output as JSON")
    log.info("Claude usage: %s", json.dumps(usage))
    if not (job_dir / "out" / "episodes.json").exists():
        raise JobError("Claude did not produce out/episodes.json")

    log.info("Generating audio")
    meta = generate.main(job_dir, SITE)
    return meta, usage


def publish(repo: str) -> None:
    run(["git", "-C", str(SITE), "add", "-A"])
    status = run(["git", "-C", str(SITE), "status", "--porcelain"])
    if status.strip():
        run(["git", "-C", str(SITE), "commit", "--quiet", "-m", f"Add podcast {repo}"])
        run(["git", "-C", str(SITE), "push", "--quiet", "origin", "gh-pages"], timeout=900)


def total_tokens(usage: dict) -> int:
    u = usage.get("usage") or {}
    return sum(v for k, v in u.items() if k.endswith("tokens") and isinstance(v, int))


def handle(issue: dict, state: dict) -> None:
    number = str(issue["number"])
    try:
        req = parse_request(issue)
    except JobError as e:
        gh("issue", "comment", number, "--body", f"❌ Invalid request: {e}")
        gh("issue", "edit", number, "--add-label", "failed")
        gh("issue", "close", number)
        return

    gh("issue", "edit", number, "--add-label", "processing")
    state["jobs"] += 1
    save_state(state)
    try:
        meta, usage = build_podcast(req)
        publish(meta["repo"])
        minutes = sum(len((SITE / ep["text"]).read_text(encoding="utf-8").split()) for ep in meta["episodes"]) // 140
        gh("issue", "comment", number, "--body",
           f"✅ {len(meta['episodes'])} episodes (~{minutes} min) ready: {PAGES_URL}\n\n"
           f"Claude: {usage.get('turns')} turns, {total_tokens(usage):,} tokens, {usage.get('duration_s')} s")
        gh("issue", "edit", number, "--remove-label", "processing", "--add-label", "done")
        log.info("Issue #%s done", number)
    except Exception as e:  # report every failure back to the app instead of crashing the loop
        log.exception("Issue #%s failed", number)
        gh("issue", "comment", number, "--body", f"❌ Failed: {str(e)[:1000]}")
        gh("issue", "edit", number, "--remove-label", "processing", "--add-label", "failed")
    gh("issue", "close", number)


def poll_once() -> None:
    issues = json.loads(gh("issue", "list", "--label", "repocast-request", "--state", "open",
                           "--json", "number,body,author,labels", "--limit", "20"))
    for issue in sorted(issues, key=lambda i: i["number"]):
        labels = {l["name"] for l in issue["labels"]}
        if labels & {"processing", "done", "failed"}:
            continue
        if issue["author"]["login"] != ALLOWED_AUTHOR:
            log.warning("Ignoring issue #%s from %s", issue["number"], issue["author"]["login"])
            continue
        state = load_state()
        if state["jobs"] >= DAILY_LIMIT:
            log.info("Daily limit of %s reached, #%s waits until tomorrow", DAILY_LIMIT, issue["number"])
            return
        log.info("Processing issue #%s", issue["number"])
        handle(issue, state)


def setup_logging() -> None:
    (SERVICE / "logs").mkdir(exist_ok=True)
    handlers = [RotatingFileHandler(SERVICE / "logs" / "watcher.log", maxBytes=1_000_000, backupCount=3, encoding="utf-8")]
    if sys.stdout:  # pythonw has no console
        handlers.append(logging.StreamHandler(sys.stdout))
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s", handlers=handlers)


def main() -> None:
    setup_logging()
    for name, color in LABELS.items():
        gh("label", "create", name, "--color", color, "--force")
    log.info("RepoCast watcher started, polling %s every %ss", QUEUE_REPO, POLL_SECONDS)
    once = "--once" in sys.argv
    while True:
        try:
            poll_once()
        except Exception:
            log.exception("Poll failed")
        if once:
            break
        time.sleep(POLL_SECONDS)


if __name__ == "__main__":
    main()
