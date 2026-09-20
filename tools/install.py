"""Install a self-contained version descriptor for vanilla-compatible launchers."""
import argparse
import copy
import json
import os
from pathlib import Path
import shutil
import sys

from common import (ROOT, VERSION, VERSION_ID, LIBRARIES, atomic_json, dependency,
                    minecraft_jar, minecraft_metadata, sha1)


def default_directory():
    if sys.platform == "win32":
        return Path(os.environ["APPDATA"]) / ".minecraft"
    if sys.platform == "darwin":
        return Path.home() / "Library/Application Support/minecraft"
    return Path.home() / ".minecraft"


def library_record(coordinate, file, url):
    group, artifact, version = coordinate.split(":")
    relative = "/".join((group.replace(".", "/"), artifact, version, artifact + "-" + version + ".jar"))
    return {"name": coordinate, "downloads": {"artifact": {
        "path": relative, "sha1": sha1(file), "size": file.stat().st_size, "url": url}}}


def make_descriptor(base, own, extra):
    result = copy.deepcopy(base)
    result.update({"id": VERSION_ID, "type": "release", "mainClass": "net.minecraft.launchwrapper.Launch",
                   "cubiClient": {"version": VERSION, "base": "1.8.9"},
                   "javaVersion": {"component": "jre-legacy", "majorVersion": 8}})
    result["minecraftArguments"] += " --tweakClass dev.cubi.launch.CubiTweaker"
    result["libraries"] = [own] + extra + result["libraries"]
    return result


def main():
    parser = argparse.ArgumentParser(description="Instala la versión CubiClient-1.8.9")
    parser.add_argument("--minecraft-dir", type=Path, default=default_directory())
    args = parser.parse_args()
    distribution = ROOT / "dist" / ("cubiclient-" + VERSION + ".jar")
    if not distribution.is_file():
        raise RuntimeError("Primero ejecuta: python3 tools/build.py --test")
    target = args.minecraft_dir.expanduser().resolve()
    version_dir = target / "versions" / VERSION_ID
    descriptor = version_dir / (VERSION_ID + ".json")
    if version_dir.exists():
        if not descriptor.is_file() or "cubiClient" not in json.loads(descriptor.read_text(encoding="utf-8")):
            raise RuntimeError("La carpeta de destino ya existe y no es una instalación reconocida de CubiClient.")
    metadata = minecraft_metadata()
    original = minecraft_jar(metadata)
    own = library_record("dev.cubi:cubiclient:" + VERSION, distribution, "")
    extras = []
    sources = [(own, distribution)]
    for name in ("launchwrapper", "asm"):
        coordinate, url, _ = LIBRARIES[name]
        source = dependency(name)
        record = library_record(coordinate, source, url)
        extras.append(record)
        sources.append((record, source))
    for record, source in sources:
        destination = target / "libraries" / record["downloads"]["artifact"]["path"]
        destination.parent.mkdir(parents=True, exist_ok=True)
        temporary = destination.with_suffix(".jar.part")
        shutil.copyfile(source, temporary)
        os.replace(str(temporary), str(destination))
    version_dir.mkdir(parents=True, exist_ok=True)
    destination = version_dir / (VERSION_ID + ".jar")
    temporary = destination.with_suffix(".jar.part")
    shutil.copyfile(original, temporary)
    os.replace(str(temporary), str(destination))
    atomic_json(descriptor, make_descriptor(metadata, own, extras))
    print("Instalado: " + str(version_dir))
    print("Reabre el launcher > Instalaciones > Nueva instalación > " + VERSION_ID)
    print("Usa Java 8. El launcher descargará los recursos y las bibliotecas originales que falten.")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, RuntimeError) as exc:
        sys.exit("Error: " + str(exc))
