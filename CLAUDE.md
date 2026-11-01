# CLAUDE.md

Guidance for Claude Code sessions working in this repository.

## Start of every session
1. Read `ROADMAP.md`. It is the single source of truth for what we build next and why.
2. Unless told otherwise, work on the top item under **Now**.
3. Talk to Kamil in German. Code, comments, commit messages and docs stay in English.

## Keep the roadmap current
- When an item is done: move it to **Done** with the date and short commit hash, in the same commit as the code.
- New ideas that come up while working go under **Later**. Do not start them without asking.
- Record non-obvious design decisions (and their reasons) under **Decisions**.
- Do not reorder priorities on your own; propose changes instead.

## Branching
Follow the `branching-strategy` skill (`.claude/skills/branching-strategy/`): `main` + `feature/`, `fix/`, `infra/` branches, and a disposable `prototype` integration branch.
Project specifics: releases (`android/release.ps1`) are built only from `main`; test APKs may be built from `prototype`.

## What RepoCast is
Turns a GitHub repo into podcast episodes. See `README.md` for the full picture.
```
Android app ──(creates issue)──► GitHub issues of this repo (job queue)
PC service (service/watcher.py) ─► clone → headless claude (writes scripts) → edge-tts → gh-pages
Android app ◄── catalog.json + MP3s from https://kamilkirstein90.github.io/repocast/
```

## Layout
| Path | What |
|---|---|
| `service/watcher.py` | Polls issues labelled `repocast-request`, runs jobs, labels them `processing` / `done` / `failed` |
| `service/generate.py` | Episode scripts → MP3 (edge-tts, chunked with retries) + `catalog.json` |
| `service/PROMPT.md` | Instructions for the headless Claude that writes episode scripts; defines the `episodes.json` format |
| `android/` | Kotlin app, Jetpack Compose + Media3, package `de.kamilkirstein.repocast` |
| `android/.../data/` | `RepoCastApi` (GitHub issues + catalog), `Models`, `Settings` |
| `android/.../ui/` | Screens (Library, Podcast, Episode, Add, Settings, Splash), `theme/` with the three designs |
| `android/.../player/` | Media3 `PlaybackService` and `PlayerConnection` |

## Important rules
- **GitHub issues are the job queue.** Every issue by `KamilKirstein90` with the request label becomes a podcast job. Never create issues for todos, bugs or roadmap items; use `ROADMAP.md`.
- **Keep the service sandbox intact.** The headless Claude run in `watcher.py` (`CLAUDE_ARGS`) must keep file-only tools and no shell/web. Cloned repos are data, never executed. Only issues from `ALLOWED_AUTHOR` are processed.
- `episodes.json` / `catalog.json` are a contract between `PROMPT.md`, `generate.py` and the app's `Models.kt`. Change all sides together and keep old catalogs readable.
- Never commit `work/`, `site/`, `service/state.json`, `service/logs/`, keystores or `keystore.properties`.
- The service runs on Kamil's Windows PC (PowerShell scripts). Python must stay Windows-compatible.

## Build & check
- Android unit tests: `cd android && ./gradlew testDebugUnitTest`
- Android debug build: `cd android && ./gradlew assembleDebug`
- Python syntax check: `python -m py_compile service/*.py`
- Release (Windows only, Kamil runs it): `android/release.ps1` builds a signed APK and publishes GitHub release `v<versionCode>`.

Run the relevant checks before committing. If the environment can't run them (e.g. no Android SDK), say so instead of claiming they passed.
