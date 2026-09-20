"""Compare two exported Cubi captures using their raw frametimes (no third-party modules)."""
import argparse
import json
import math
from pathlib import Path


def summarize(capture):
    frames = capture.get("frameNanos")
    if capture.get("schema") != 1 or not isinstance(frames, list) or not frames:
        raise ValueError("Captura vacía o esquema no compatible")
    if len(frames) != capture.get("samples") or len(frames) > 131072:
        raise ValueError("Cantidad de muestras inconsistente")
    if any(type(value) is not int or value <= 0 for value in frames):
        raise ValueError("Tiempos de fotograma inválidos")
    ordered = sorted(frames)
    seconds = sum(frames) / 1e9
    return {
        "seconds": seconds,
        "fps": len(frames) / seconds,
        "p95": ordered[math.ceil(len(frames) * 0.95) - 1] / 1e6,
        "p99": ordered[math.ceil(len(frames) * 0.99) - 1] / 1e6,
    }


def main():
    parser = argparse.ArgumentParser(description="Compara dos capturas de Rendimiento > Diagnóstico")
    parser.add_argument("before", type=Path)
    parser.add_argument("after", type=Path)
    args = parser.parse_args()
    captures = [json.loads(path.read_text(encoding="utf-8")) for path in (args.before, args.after)]
    before, after = map(summarize, captures)
    print("Métrica                     Antes       Después       Cambio")
    for key, label in (("fps", "FPS medios (más = mejor)"), ("p95", "p95 ms (menos = mejor)"), ("p99", "p99 ms (menos = mejor)")):
        change = (after[key] / before[key] - 1) * 100
        print(f"{label:26} {before[key]:10.2f} {after[key]:12.2f} {change:+10.2f}%")
    print(f"Duración de muestras: {before['seconds']:.2f} s / {after['seconds']:.2f} s")
    contexts = [capture.get("context", {}) for capture in captures]
    changed = [key for key in ("gpu", "driver", "os", "java", "processors", "maxHeapBytes", "width", "height", "video", "hudVisible")
               if contexts[0].get(key) != contexts[1].get(key)]
    print("Contexto: " + ("cambian " + ", ".join(changed) if changed else "coinciden hardware y opciones de vídeo registradas"))
    print("Escena, recorrido y paquetes de recursos deben coincidir para atribuir el cambio a la optimización.")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, TypeError) as error:
        raise SystemExit("Error: " + str(error))
