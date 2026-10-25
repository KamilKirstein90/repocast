# RepoCast

Turns a GitHub repository into a series of podcast episodes that explain it step by step: what it is, how to install it, its architecture and main features. In **Learn** mode it adds episodes on concepts, a code walkthrough and an honest code review.

```
Android app ──(creates issue)──► KamilKirstein90/repocast issues (job queue)
PC service (service/watcher.py) polls every 60 s
   └─ git clone → claude -p (sandboxed, writes scripts) → edge-tts (MP3) → gh-pages
Android app ◄──(catalog.json + MP3s)── https://kamilkirstein90.github.io/repocast/
```

## Parts
| Folder | What |
|---|---|
| `service/PROMPT.md` | Instructions for the episode scripts |
| `service/watcher.py` | Polls the issue queue and builds podcasts (daily limit 3) |
| `service/generate.py` | Scripts → MP3 (edge-tts) + `catalog.json` |
| `android/` | Kotlin app (Jetpack Compose + Media3) |
| `site/` | Local worktree of the `gh-pages` branch (not in `main`) |

## Security
- The PC opens no port; it only talks to GitHub.
- Only issues authored by `KamilKirstein90` are processed; the issue body is parsed as data.
- Claude runs with `--restricted --tools Read,Glob,Grep,Write`: no shell, no web, writes only inside the job folder. Cloned repos are never executed.

## Setup
### PC service
```powershell
pip install edge-tts
git worktree add site gh-pages      # once
python service\watcher.py --once    # single test run
powershell -File service\install-autostart.ps1   # start at Windows login
```
Logs: `service/logs/watcher.log`.

### App token (once)
1. github.com → Settings → Developer settings → Personal access tokens → **Fine-grained tokens** → Generate new token.
2. Repository access: **Only select repositories** → `repocast`.
3. Permissions → Repository permissions → **Issues: Read and write**. Nothing else.
4. Copy the token into the app under Settings. You can revoke it on GitHub at any time.
