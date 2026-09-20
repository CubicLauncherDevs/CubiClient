"""Compile Cubi and optionally prepare a complete replacement jar for Prism Launcher."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile

from common import ROOT, VERSION, dependency, font_asset, minecraft_jar, minecraft_metadata
from replacement import build_replacement


def java_tool(name):
    home = os.environ.get("JAVA_HOME")
    suffix = ".exe" if os.name == "nt" else ""
    tool = str(Path(home) / "bin" / (name + suffix)) if home else shutil.which(name)
    if not tool:
        raise RuntimeError("Instala un JDK 8 y configura JAVA_HOME; falta " + name)
    return tool


def compile_sources(javac, sources, output, classpath):
    output.mkdir(parents=True, exist_ok=True)
    subprocess.run([javac, "-encoding", "UTF-8", "-source", "8", "-target", "8",
                    "-Xlint:all", "-cp", classpath, "-d", str(output)]
                   + [str(p) for p in sources], check=True)


def main():
    parser = argparse.ArgumentParser(description="Compila CubiClient con JDK 8")
    parser.add_argument("--test", action="store_true", help="Prueba configuración, CPS y bytecode real 1.8.9")
    parser.add_argument("--replacement", action="store_true", help="Genera dist/replacement/minecraft.jar para Prism Launcher")
    args = parser.parse_args()
    javac = java_tool("javac")
    version = subprocess.run([javac, "-version"], capture_output=True, text=True, check=True)
    if "1.8." not in version.stdout + version.stderr:
        raise RuntimeError("Se requiere JDK 8 (JAVA_HOME). Evita APIs incompatibles con Minecraft 1.8.9.")
    classpath = os.pathsep.join(str(dependency(name)) for name in ("launchwrapper", "asm", "lwjgl", "gson"))
    classes = ROOT / "build" / "classes"
    if classes.exists():
        shutil.rmtree(classes)
    compile_sources(javac, sorted((ROOT / "src/main/java").rglob("*.java")), classes, classpath)
    build_classes = ROOT / "build" / "build-classes"
    if build_classes.exists():
        shutil.rmtree(build_classes)
    tool_cp = str(classes) + os.pathsep + classpath
    compile_sources(javac, sorted((ROOT / "src/build/java").rglob("*.java")), build_classes, tool_cp)
    metadata = minecraft_metadata() if args.replacement or args.test else None
    original = minecraft_jar(metadata) if metadata else None
    patched = ROOT / "build" / "patched"
    prepare = [java_tool("java"), "-cp", str(build_classes) + os.pathsep + tool_cp,
               "dev.cubi.build.PrepareClasses", str(classes)]
    if args.replacement:
        prepare += [str(original), str(patched)]
    subprocess.run(prepare, check=True)
    subprocess.run([java_tool("java"), "-Djava.awt.headless=true", "-cp", str(build_classes),
                    "dev.cubi.build.UiAssets", str(classes), str(font_asset("Cantarell-Regular.ttf")),
                    str(font_asset("Cantarell-Bold.ttf"))], check=True)
    shutil.copyfile(font_asset("OFL.txt"), classes / "assets/cubi/ui/OFL-Cantarell.txt")
    distribution = ROOT / "dist" / ("cubiclient-" + VERSION + ".jar")
    distribution.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(distribution, "w", zipfile.ZIP_DEFLATED) as jar:
        jar.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nImplementation-Title: CubiClient\nImplementation-Version: " + VERSION + "\n\n")
        for source in sorted(classes.rglob("*")):
            if source.is_file():
                jar.write(source, source.relative_to(classes).as_posix())
        resources = ROOT / "src/main/resources"
        if resources.exists():
            for source in sorted(resources.rglob("*")):
                if source.is_file():
                    jar.write(source, source.relative_to(resources).as_posix())
    if args.test:
        test_classes = ROOT / "build" / "test-classes"
        if test_classes.exists():
            shutil.rmtree(test_classes)
        test_cp = str(build_classes) + os.pathsep + tool_cp
        compile_sources(javac, sorted((ROOT / "src/test/java").rglob("*.java")), test_classes, test_cp)
        subprocess.run([java_tool("java"), "-Djava.awt.headless=true", "-Xverify:all", "-cp", str(test_classes) + os.pathsep + test_cp,
                        "dev.cubi.tests.SelfTest", str(original)], check=True)
    print("Listo: " + str(distribution))
    if args.replacement:
        replacement = build_replacement(original, distribution, patched, ROOT / "dist/replacement/minecraft.jar")
        if args.test:
            from verify_runtime import verify_runtime
            verify_runtime(java_tool("java"), replacement, metadata)
        print("Prism > Editar > Versión > Reemplazar Minecraft.jar: " + str(replacement))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, RuntimeError, subprocess.CalledProcessError) as exc:
        sys.exit("Error: " + str(exc))
