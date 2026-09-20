"""Headless JVM verification of the final replacement; never starts Minecraft."""
import os
import platform
import subprocess

from common import CACHE, ROOT, download
from smoke import allowed


def verify_runtime(java, replacement, metadata):
    system = {"Linux": "linux", "Darwin": "osx", "Windows": "windows"}[platform.system()]
    classpath = [ROOT / "build/test-classes", replacement]
    for library in metadata["libraries"]:
        artifact = library.get("downloads", {}).get("artifact")
        if artifact and allowed(library, system):
            classpath.append(download(artifact["url"], CACHE / "libraries" / artifact["path"], artifact["sha1"]))
    subprocess.run([java, "-Djava.awt.headless=true", "-Xverify:all", "-cp", os.pathsep.join(map(str, classpath)),
                    "dev.cubi.tests.LinkageTest", str(replacement)], check=True)
