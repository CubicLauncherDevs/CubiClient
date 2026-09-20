"""Build and validate the complete jar consumed by Prism's Replace Minecraft.jar action."""
import json
import os
from pathlib import Path
import zipfile

from common import VERSION, sha1

PATCHED_CLASSES = ("ave.class", "avo.class", "aya.class")
MAIN_CLASS = "net/minecraft/client/main/Main.class"
SCREEN_CLASS = "cubi/generated/ControlScreen.class"


def obsolete_metadata(name):
    upper = name.upper()
    if upper in ("META-INF/MANIFEST.MF", "META-INF/INDEX.LIST"):
        return True
    # Original jar signatures/digests no longer describe the patched classes.
    if upper.startswith("META-INF/") and upper.count("/") == 1:
        return upper.endswith((".SF", ".RSA", ".DSA", ".EC")) or upper.startswith("META-INF/SIG-")
    return False


def validate_replacement(original, output):
    with zipfile.ZipFile(original) as vanilla, zipfile.ZipFile(output) as result:
        names = result.namelist()
        if len(names) != len(set(names)):
            raise ValueError("El jar de reemplazo tiene entradas duplicadas")
        if result.testzip() is not None:
            raise ValueError("El jar de reemplazo tiene datos corruptos")
        required = (MAIN_CLASS, SCREEN_CLASS, "dev/cubi/core/Hooks.class", "dev/cubi/bridge/Game189.class")
        required += ("assets/cubi/ui/atlas.png", "assets/cubi/ui/atlas.bin", "assets/cubi/ui/OFL-Lato.txt")
        for name in required:
            if name not in names:
                raise ValueError("Falta una clase necesaria: " + name)
        for entry in vanilla.infolist():
            if entry.is_dir() or obsolete_metadata(entry.filename):
                continue
            before = vanilla.read(entry)
            after = result.read(entry.filename)
            if entry.filename in PATCHED_CLASSES:
                if before == after or b"dev/cubi/core/Hooks" not in after:
                    raise ValueError("No se aplicaron los hooks: " + entry.filename)
            elif before != after:
                raise ValueError("Se modificó un recurso ajeno al parche: " + entry.filename)
        for name in names:
            if obsolete_metadata(name) and name != "META-INF/MANIFEST.MF":
                raise ValueError("Quedan firmas/índices obsoletos: " + name)
            if name.startswith(("dev/cubi/launch/", "dev/cubi/build/", "dev/cubi/tests/",
                                "net/minecraft/launchwrapper/", "org/objectweb/asm/")):
                raise ValueError("Dependencia o herramienta innecesaria en el jar: " + name)
            if name.endswith(".class") and name.startswith(("dev/cubi/", "cubi/")):
                data = result.read(name)
                if b"net/minecraft/launchwrapper" in data or b"org/objectweb/asm" in data:
                    raise ValueError("Dependencia de ejecución pendiente en " + name)
        manifest = result.read("META-INF/MANIFEST.MF").decode("utf-8")
        if "Main-Class: net.minecraft.client.main.Main\r\n" not in manifest:
            raise ValueError("Punto de entrada de Minecraft incorrecto")
    print("PASS / Replacement: recursos originales intactos, hooks aplicados, sin dependencia de LaunchWrapper/ASM.")


def build_replacement(original, library, patched, output):
    output = Path(output)
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix(".jar.part")
    try:
        with zipfile.ZipFile(original) as vanilla, zipfile.ZipFile(library) as cubi, \
                zipfile.ZipFile(temporary, "w", zipfile.ZIP_DEFLATED) as result:
            written = set()

            def add(name, data):
                if name in written:
                    raise ValueError("Colisión de entradas al empaquetar: " + name)
                written.add(name)
                result.writestr(name, data)

            manifest = ("Manifest-Version: 1.0\r\nMain-Class: net.minecraft.client.main.Main\r\n"
                        "Implementation-Title: CubiClient\r\nImplementation-Version: " + VERSION + "\r\n\r\n")
            add("META-INF/MANIFEST.MF", manifest)
            for entry in vanilla.infolist():
                if entry.is_dir() or obsolete_metadata(entry.filename):
                    continue
                data = (Path(patched) / entry.filename).read_bytes() if entry.filename in PATCHED_CLASSES else vanilla.read(entry)
                add(entry.filename, data)
            for entry in cubi.infolist():
                if entry.is_dir() or obsolete_metadata(entry.filename) or entry.filename.startswith("dev/cubi/launch/"):
                    continue
                add(entry.filename, cubi.read(entry))
            add("META-INF/cubi/build.json", json.dumps({
                "version": VERSION, "minecraft": "1.8.9", "format": "prism-replacement",
                "originalSha1": sha1(original), "patchedClasses": list(PATCHED_CLASSES),
                "mainClass": "net.minecraft.client.main.Main",
            }, indent=2) + "\n")
        validate_replacement(original, temporary)
        os.replace(str(temporary), str(output))
    finally:
        temporary.unlink(missing_ok=True)
    return output
