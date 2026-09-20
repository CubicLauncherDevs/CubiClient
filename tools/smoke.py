"""Optional real-window integration test. No account or multiplayer; isolated test directory."""
import argparse
import os
from pathlib import Path
import platform
import signal
import subprocess
import sys
import zipfile

from build import java_tool
from common import ROOT, CACHE, download, dependency, minecraft_jar, minecraft_metadata


def run_game(command, log, timeout=210, grace=5):
    """Keep logs live and reap only this test's child on cancellation or timeout."""
    parent = os.getpid()

    def die_with_parent():
        # The Linux harness can terminate Python without running its finally block.
        # This process-local setting prevents the test JVM from surviving that case.
        import ctypes
        libc = ctypes.CDLL(None, use_errno=True)
        if libc.prctl(1, signal.SIGKILL, 0, 0, 0) != 0:  # PR_SET_PDEATHSIG
            raise OSError(ctypes.get_errno(), "Could not guard the test process")
        if os.getppid() != parent:
            os.kill(os.getpid(), signal.SIGKILL)

    def interrupted(signum, frame):
        raise KeyboardInterrupt("Prueba cancelada")

    handlers = {sig: signal.getsignal(sig) for sig in (signal.SIGINT, signal.SIGTERM)}
    process = None
    try:
        for sig in handlers:
            signal.signal(sig, interrupted)
        with Path(log).open("wb") as output:
            process = subprocess.Popen(command, stdout=output, stderr=subprocess.STDOUT,
                                       cwd=str(Path(log).resolve().parent),
                                       preexec_fn=die_with_parent if sys.platform == "linux" else None)
            return process.wait(timeout=timeout)
    finally:
        for sig in handlers:
            signal.signal(sig, signal.SIG_IGN)
        try:
            if process is not None and process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=grace)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()
        finally:
            for sig, handler in handlers.items():
                signal.signal(sig, handler)


def allowed(library, system):
    rules = library.get("rules")
    if not rules:
        return True
    result = False
    for rule in rules:
        constraint = rule.get("os", {})
        if constraint.get("name", system) == system:
            result = rule["action"] == "allow"
    return result


def main():
    parser = argparse.ArgumentParser(description="Prueba gráfica aislada de CubiClient")
    parser.add_argument("--replacement", action="store_true", help="Prueba el minecraft.jar de Prism sin LaunchWrapper ni ASM")
    args = parser.parse_args()
    if not (ROOT / "build/test-classes/dev/cubi/tests/SmokeTest.class").is_file():
        raise RuntimeError("Primero ejecuta python3 tools/build.py --test")
    metadata = minecraft_metadata()
    replacement = ROOT / "dist/replacement/minecraft.jar"
    if args.replacement and not replacement.is_file():
        raise RuntimeError("Primero ejecuta python3 tools/build.py --replacement --test")
    game_dir = ROOT / ("run/smoke-replacement" if args.replacement else "run/smoke")
    system = {"Linux": "linux", "Darwin": "osx", "Windows": "windows"}[platform.system()]
    natives = game_dir / "natives"
    natives.mkdir(parents=True, exist_ok=True)
    # This directory belongs to the automated test, not the launcher's game directory.
    (game_dir / "options.txt").write_text(
        "guiScale:2\nfullscreen:false\nrenderDistance:4\nmaxFps:120\npauseOnLostFocus:false\n", encoding="utf-8")
    classpath = [ROOT / "build/test-classes"]
    if args.replacement:
        classpath.append(replacement)
    else:
        classpath += [ROOT / "build/classes", minecraft_jar(metadata), dependency("launchwrapper"), dependency("asm")]
    for library in metadata["libraries"]:
        if not allowed(library, system):
            continue
        artifact = library.get("downloads", {}).get("artifact")
        if artifact:
            classpath.append(download(artifact["url"], CACHE / "libraries" / artifact["path"], artifact["sha1"]))
        classifier = library.get("natives", {}).get(system)
        if classifier:
            classifier = classifier.replace("${arch}", "64" if sys.maxsize > 2**32 else "32")
            native = library["downloads"]["classifiers"][classifier]
            source = download(native["url"], CACHE / "libraries" / native["path"], native["sha1"])
            with zipfile.ZipFile(source) as archive:
                for entry in archive.infolist():
                    if entry.filename.endswith((".so", ".dll", ".dylib", ".jnilib")):
                        (natives / Path(entry.filename).name).write_bytes(archive.read(entry))
    # Game textures are inside the original jar; audio assets aren't needed for this test.
    assets = CACHE / "assets"
    index = metadata["assetIndex"]
    download(index["url"], assets / "indexes" / (index["id"] + ".json"), index["sha1"])
    jvm = [java_tool("java"), "-Xms256M", "-Xmx1G", "-Djava.library.path=" + str(natives)]
    if args.replacement:
        jvm += ["-Dcubi.smoke.replacement=true", "-Dcubi.smoke.jar=" + str(replacement)]
    logging = metadata.get("logging", {}).get("client")
    if logging:
        config = logging["file"]
        path = download(config["url"], CACHE / config["id"], config["sha1"])
        jvm.append(logging["argument"].replace("${path}", str(path)))
    command = jvm + ["-cp", os.pathsep.join(map(str, classpath)), "dev.cubi.tests.SmokeTest"]
    if not args.replacement:
        command += ["--tweakClass", "dev.cubi.launch.CubiTweaker"]
    command += ["--version", "CubiClient-1.8.9",
                     "--username", "CubiDev", "--accessToken", "0", "--userProperties", "{}",
                     "--uuid", "00000000000000000000000000000000", "--userType", "legacy",
                     "--gameDir", str(game_dir), "--assetsDir", str(assets), "--assetIndex", index["id"],
                     "--width", "1100", "--height", "700"]
    log = game_dir / "smoke.log"
    returncode = run_game(command, log)
    text = log.read_text(encoding="utf-8", errors="replace")
    for line in text.splitlines():
        if "[Cubi]" in line or "PASS /" in line:
            print(line)
    markers = ("PASS / OpenGL:", "PASS / WORLD:", "PASS / KEYBOARD:", "PASS / PERFORMANCE:", "PASS / CAPTURE:")
    if returncode or any(marker not in text for marker in markers):
        raise RuntimeError("Smoke test falló. Consulta " + str(log))
    print("Capturas: " + str(game_dir / "screenshots"))


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit("Prueba cancelada. Consulta el registro en la carpeta de pruebas.")
    except (OSError, ValueError, RuntimeError, subprocess.SubprocessError) as error:
        sys.exit("Error: " + str(error))
