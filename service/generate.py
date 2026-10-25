"""Turn the episode scripts of one job into MP3s and register them in the site catalog.

Usage: python generate.py <job_dir> <site_dir>
  job_dir  contains job.json and out/ (scripts/*.txt + episodes.json) written by Claude
  site_dir the folder published to gh-pages (catalog.json + podcasts/)
"""
import asyncio
import json
import shutil
import sys
from datetime import datetime, timezone
from pathlib import Path

import edge_tts

VOICES = {"de": "de-DE-ConradNeural", "en": "en-US-AndrewNeural"}


async def synthesize(text: str, voice: str, target: Path) -> None:
    await edge_tts.Communicate(text, voice).save(str(target))


def main(job_dir: Path, site_dir: Path) -> dict:
    job = json.loads((job_dir / "job.json").read_text(encoding="utf-8-sig"))
    meta = json.loads((job_dir / "out" / "episodes.json").read_text(encoding="utf-8"))
    voice = VOICES.get(job["lang"], VOICES["en"])

    slug = meta["repo"].replace("/", "__")
    target = site_dir / "podcasts" / slug
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True)

    for ep in meta["episodes"]:
        script = job_dir / "out" / "scripts" / f"{ep['file']}.txt"
        text = script.read_text(encoding="utf-8")
        print(f"TTS {ep['file']} ({len(text.split())} words)")
        asyncio.run(synthesize(text, voice, target / f"{ep['file']}.mp3"))
        shutil.copy(script, target / f"{ep['file']}.txt")
        ep["audio"] = f"podcasts/{slug}/{ep['file']}.mp3"
        ep["text"] = f"podcasts/{slug}/{ep['file']}.txt"

    meta.update(url=job["url"], sha=job["sha"], voice=voice,
                created=datetime.now(timezone.utc).isoformat(timespec="seconds"))
    (target / "episodes.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8")

    catalog_path = site_dir / "catalog.json"
    catalog = json.loads(catalog_path.read_text(encoding="utf-8")) if catalog_path.exists() else {"podcasts": []}
    entry = {k: meta[k] for k in ("repo", "title", "description", "lang", "mode", "created")}
    entry.update(episodes=len(meta["episodes"]), path=f"podcasts/{slug}/episodes.json")
    catalog["podcasts"] = [p for p in catalog["podcasts"] if p["repo"] != meta["repo"]] + [entry]
    catalog_path.write_text(json.dumps(catalog, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Done: {len(meta['episodes'])} episodes -> {target}")
    return meta


if __name__ == "__main__":
    main(Path(sys.argv[1]), Path(sys.argv[2]))
