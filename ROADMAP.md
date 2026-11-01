# RepoCast Roadmap

## Vision
Listen your way into any GitHub repo: RepoCast turns a repository into a short podcast series
that explains what it is, how it's built and how good the code is, for listening on the go.

## Now
_Being worked on next, in this order._
1. **Show job progress live in the app.** Right now you only know a request is "running". Show the stages (cloning → writing scripts → audio → published) from issue labels/comments.

## Next
- **Refresh a podcast when the repo changed.** Detect a new commit SHA and offer to regenerate.
- **Offline listening.** Download episodes for listening without network.
- **Playback speed and episode chapters.**
- **Show code snippets while listening.** Display `snippets`/`commands` from `episodes.json` on the episode screen in sync with playback.

## Later
- CI: run Android unit tests and a Python check on every push.
- Tests for `service/generate.py` (chunking, catalog writing).

## Decisions
- 2026-10-30: Dropped Material You wallpaper colors; they gave the app a brown look. Three fixed designs instead (Nachtradio, Leinen, Dämmerung).
- 2026-11-01: The plan lives in this file, not in GitHub issues, because issues are the podcast job queue.
- 2026-11-01: Branching: `main` + short-lived `feature/`, `fix/`, `infra/` branches, plus a disposable `prototype` branch that collects in-progress features for testing but never merges into `main`. Defined in the reusable `branching-strategy` skill.

## Done
- 2026-10-30 `4ce822b`: Three selectable designs, animated intro, new home screen.
- `72839f0`: Robust TTS (chunked synthesis with retries), script reuse on retry, CBR seeking in player.
- `760390b`: Kotlin Android app (Compose + Media3), release and autostart scripts.
- `4cfafb8`: RepoCast service: prompt, TTS generator, issue-queue watcher.
