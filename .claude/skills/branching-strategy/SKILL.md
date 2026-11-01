---
name: branching-strategy
description: Kamil's git branching strategy (main + feature/fix/infra branches + a disposable prototype integration branch). Use whenever starting work that will be committed, choosing or naming a branch, opening or merging a pull request, deciding what goes to main, or when asked to update, rebuild or test the prototype branch.
---

# Branching strategy

## Branches

| Branch | Lifetime | Purpose | Branches from | Merges into |
|---|---|---|---|---|
| `main` | permanent | Always working and releasable. Releases are built only from here. | – | – |
| `feature/<name>` | short | One feature | `main` | `main` (via PR) and `prototype` |
| `fix/<name>` | short | One bug fix | `main` | `main` (via PR), `prototype` if relevant |
| `infra/<name>` | short | Build, CI, scripts, tooling, docs such as CLAUDE.md / ROADMAP.md | `main` | `main` (via PR) |
| `prototype` | permanent but disposable | Integration branch: all in-progress features together, for trying them out side by side | rebuilt from `main` | **never merges anywhere** |

`<name>` is short kebab-case describing the work, e.g. `feature/job-progress`, `fix/player-seek`, `infra/ci-tests`.
One branch = one topic. Don't mix a fix into a feature branch; open a separate `fix/` branch.

## Rules
1. **Never commit directly to `main`** unless the user explicitly says so for that change. Everything reaches `main` through a pull request from a `feature/`, `fix/` or `infra/` branch.
2. **`prototype` is a target, never a source.** Merge feature branches *into* it; never merge `prototype` into `main` or into a feature branch, and never branch off it. Otherwise unfinished work from other features leaks into `main`.
3. **No own work on `prototype`.** Fixes for a feature are made on that feature's branch and then merged into `prototype` again.
4. **No releases from `prototype`.** It may produce test builds (e.g. a debug APK) only.
5. Keep short-lived branches current by merging or rebasing `main` into them before opening the PR. Rebase only branches nobody else works on.
6. After a PR is merged, delete its branch and rebuild `prototype` if that branch was in it.

## Workflow

### Starting work
1. Decide the type: new behaviour → `feature/`, broken behaviour → `fix/`, everything around the product → `infra/`.
2. `git fetch origin main && git checkout -b <type>/<name> origin/main`
3. If the session already has a **designated branch** it must push to (e.g. a cloud session's `claude/...` branch), use that branch instead of creating one. Tell the user which type the work is and that the PR targets `main`. Suggest starting future sessions on a `<type>/<name>` branch.

### Finishing work
1. Run the project's checks (see its CLAUDE.md).
2. Push the branch and, **only when the user asks**, open a PR into `main`. The PR title starts with the type, e.g. `feature: show job progress`.
3. Prefer **squash merge** for `feature/` and `fix/` so `main` gets one clean commit per topic.

### Updating `prototype` (add one feature)
```bash
git fetch origin
git checkout prototype && git pull origin prototype
git merge --no-ff origin/feature/<name>
git push origin prototype
```
If `prototype` doesn't exist yet: `git checkout -b prototype origin/main`, then merge as above.

### Rebuilding `prototype` (after merges to main, or when it got messy or conflicted)
1. List the candidates: `git branch -r --no-merged origin/main --list 'origin/feature/*' 'origin/fix/*'`
2. Ask the user which of them should be in `prototype` (default: all listed).
3. Rebuild:
   ```bash
   git checkout -B prototype origin/main
   git merge --no-ff origin/feature/a
   git merge --no-ff origin/feature/b
   git push --force-with-lease origin prototype
   ```
4. The force push rewrites `prototype`; get the user's OK first. Resolve merge conflicts in the feature branches when possible, not only in `prototype`, so the fix survives the next rebuild.

## Commits
- English, imperative subject line ≤ 72 characters ("Add job progress card", not "Added …").
- Body explains *why* when it isn't obvious.
- Never commit secrets, keystores, build output or local state files.
