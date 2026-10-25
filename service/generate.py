"""Turn the episode scripts of one job into MP3s and register them in the site catalog.

Usage: python generate.py <job_dir> <site_dir>
  job_dir  contains job.json and out/ (scripts/*.txt + episodes.json) written by Claude
  site_dir the folder published to gh-pages (catalog.json + podcasts/)
"""
import asyncio
import json
import shutil
import sys
import unicodedata
from datetime import datetime, timezone
from pathlib import Path

import edge_tts

VOICES = {"de": "de-DE-ConradNeural", "en": "en-US-AndrewNeural"}


# edge-tts is an unofficial service that randomly returns no audio now and then. Small chunks
# keep each retry cheap; the MP3 pieces share one format, so they can simply be concatenated.
CHUNK_CHARS = 1200
RETRY_DELAYS = [2, 5, 15, 30, 60, 120]


def speakable(text: str) -> str:
    """Drop letters from non-Latin scripts (e.g. Cyrillic examples) that the voice cannot read."""
    return "".join(
        ch for ch in text
        if not unicodedata.category(ch).startswith("L") or unicodedata.name(ch, "").startswith("LATIN")
    )


def chunks(text: str) -> list[str]:
    """Group paragraphs into pieces of roughly CHUNK_CHARS."""
    pieces, current = [], ""
    for para in (p.strip() for p in text.split("\n\n")):
        if current and len(current) + len(para) > CHUNK_CHARS:
            pieces.append(current)
            current = ""
        current = f"{current}\n\n{para}" if current else para
    return [*pieces, current] if current else pieces


async def synthesize_chunk(text: str, voice: str) -> bytes:
    for attempt, delay in enumerate([0, *RETRY_DELAYS]):
        await asyncio.sleep(delay)
        try:
            audio = bytearray()
            async with asyncio.timeout(90):
                async for message in edge_tts.Communicate(text, voice).stream():
                    if message["type"] == "audio":
                        audio.extend(message["data"])
            return bytes(audio)
        except Exception as e:
            print(f"  attempt {attempt + 1} failed: {e!r}")
    raise RuntimeError(f"Text-to-speech failed after {len(RETRY_DELAYS) + 1} attempts: {text[:80]!r}")


async def synthesize(text: str, voice: str, target: Path) -> None:
    audio = b"".join([await synthesize_chunk(piece, voice) for piece in chunks(text)])
    target.write_bytes(audio)


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
        asyncio.run(synthesize(speakable(text), voice, target / f"{ep['file']}.mp3"))
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
