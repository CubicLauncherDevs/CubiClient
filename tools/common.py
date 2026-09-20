"""Shared, dependency-free build/installation helpers (Python 3.9+)."""
import hashlib
import json
import os
from pathlib import Path
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / ".cache"
VERSION = "0.2.1"
VERSION_ID = "CubiClient-1.8.9"
LIBRARIES = {
    "launchwrapper": (
        "net.minecraft:launchwrapper:1.12",
        "https://libraries.minecraft.net/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar",
        "111e7bea9c968cdb3d06ef4632bf7ff0824d0f36",
    ),
    "asm": (
        "org.ow2.asm:asm-all:5.2",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-all/5.2/asm-all-5.2.jar",
        "2ea49e08b876bbd33e0a7ce75c8f371d29e1f10a",
    ),
    "lwjgl": (
        "org.lwjgl.lwjgl:lwjgl:2.9.4-nightly-20150209",
        "https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar",
        "697517568c68e78ae0b4544145af031c81082dfe",
    ),
    "gson": (
        "com.google.code.gson:gson:2.2.4",
        "https://libraries.minecraft.net/com/google/code/gson/gson/2.2.4/gson-2.2.4.jar",
        "a60a5e993c98c864010053cb901b7eab25306568",
    ),
}

FONT_ASSETS = {
    "Lato-Regular.ttf": "29eb192629b0bbb41a7b7f49ab2aec82d4261921",
    "Lato-Bold.ttf": "a3a53a436baaf6dc2e7a05f05866a761c214692b",
    "OFL.txt": "76897b37e127e2332a1a79aab2e0d6f30ccdc47a",
}


def font_asset(name):
    return download("https://raw.githubusercontent.com/google/fonts/main/ofl/lato/" + name,
                    CACHE / "fonts" / name, FONT_ASSETS[name])


def sha1(path):
    digest = hashlib.sha1()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def download(url, destination, expected=None):
    destination = Path(destination)
    if destination.is_file() and expected and sha1(destination) == expected:
        return destination
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(destination.suffix + ".part")
    for attempt in range(3):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": "CubiClient/0.1"})
            with urllib.request.urlopen(request, timeout=60) as response, temporary.open("wb") as out:
                while True:
                    block = response.read(1024 * 1024)
                    if not block:
                        break
                    out.write(block)
            if expected and sha1(temporary) != expected:
                raise ValueError("Checksum incorrecto: " + url)
            os.replace(str(temporary), str(destination))
            return destination
        except (OSError, ValueError):
            temporary.unlink(missing_ok=True)
            if attempt == 2:
                raise
            time.sleep(attempt + 1)


def dependency(name):
    _, url, checksum = LIBRARIES[name]
    return download(url, CACHE / url.rsplit("/", 1)[-1], checksum)


def minecraft_metadata():
    manifest = download("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json",
                        CACHE / "version_manifest_v2.json")
    versions = json.loads(manifest.read_text(encoding="utf-8"))["versions"]
    entry = next(v for v in versions if v["id"] == "1.8.9")
    path = download(entry["url"], CACHE / "1.8.9.json", entry["sha1"])
    return json.loads(path.read_text(encoding="utf-8"))


def minecraft_jar(metadata):
    artifact = metadata["downloads"]["client"]
    return download(artifact["url"], CACHE / "minecraft-1.8.9.jar", artifact["sha1"])


def atomic_json(path, data):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".json.part")
    temporary.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    os.replace(str(temporary), str(path))
