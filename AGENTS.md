# CubiClient agent notes

## Fixed project constraints
- Keep `tools/common.py:VERSION` at **0.0.1**, including documentation and artifact metadata, unless the user explicitly requests a version change. Configuration/theme revisions are independent.
- Target **vanilla Minecraft 1.8.9, Java 8**. This is a replacement client jar, not a Forge mod. Preserve the full visible name through `ClientIdentity.NAME` (`CubiClient`).
- Prism's **Replace Minecraft.jar** takes `dist/replacement/minecraft.jar`. The smaller `dist/cubiclient-0.0.1.jar` is an optional LaunchWrapper library; `tools/install.py` installs that format into a Minecraft versions directory, not a Prism instance. Prism copies the replacement jar: reselect it after rebuilding.

## Build and verification
- Use **Python 3.9+ and JDK 8**, not Gradle/Maven. `tools/build.py` rejects other `javac` versions; `JAVA_HOME` overrides PATH. Dependencies and fonts are hash-pinned in `tools/common.py` and cached under `.cache/`.
- Run from the repository root:
  ```bash
  python3 tools/build.py --replacement --test
  python3 -m unittest discover -s tools -p 'test_*.py'
  ```
- Java tests are the headless `SelfTest` main (including `DeckTest`), not JUnit. `--test` compiles/runs them after codegen and checks bytecode against Mojang's original jar. Omitting `--replacement` does **not** refresh the Prism artifact.
- Focused Python tests: `python3 tools/test_installer.py` or `python3 tools/test_smoke_process.py`.
- Coordinate with the user before `python3 tools/smoke.py --replacement`; first build with `--replacement --test`. It opens a real OpenGL window, creates a local world and suppresses physical keyboard/mouse events in that test process. A previous run was disruptive. Use the runner's cleanup, not an unmanaged Java launch; logs/screenshots go to `run/smoke-replacement/`.
- Headless success is not in-game verification. Check `docs/VERIFICATION.md` before claiming graphical coverage; existing screenshots can predate the current UI.

## Integration traps
- `PrepareClasses` applies `CubiTransformer` to `ave`, `avo`, `aya` at build time. The replacement starts at `net.minecraft.client.main.Main` with vanilla libraries; `tools/replacement.py` rejects runtime ASM/LaunchWrapper references in Cubi classes. Keep build-only code out of runtime paths.
- Put Minecraft access in `bridge/Game189` using its cached handles and runtime loader; update binding checks in `SelfTest` when adding/changing obfuscated access. There is no decompiled Minecraft source workspace.
- `ScreenGenerator` emits `cubi.generated.ControlScreen`. Its namespace must remain outside `dev.cubi.*`, which the optional `CubiTweaker` excludes from its game classloader.
- **Do not open the menu directly inside keyboard dispatch.** Vanilla forwards the same event to the newly opened screen, immediately closing it. Preserve `MenuActivation`'s end-of-tick opening and the consumed-event/repeat guards in `Hooks` and `ControlDeck`.

## Editing UI and generated assets
- `src/build/java/` is tracked source for screen/atlas generation. Root `/build/`, `dist/`, `.cache/`, and `run/` are outputs. Keep the leading slash in `.gitignore`'s `/build/`: `build/` previously hid the source generators.
- Edit `UiAssets` and rebuild rather than editing generated classes, atlas PNGs or metrics. Font changes must include pinned downloads and the bundled license expected by `tools/replacement.py`.
- Keep palette/radii in `Theme`, navigation in `DeckState`, and shared drawing/hit regions in `DeckLayout`. The grid currently has three slots: adding a module requires updating both `ModuleRegistry` and the layout/interaction tests.
- Use `Ink`/`UiAtlas` and `Game189`'s cached `GlStateManager` access to keep vanilla's graphics-state cache synchronized. Font rasterization belongs in codegen, not the render loop.
- Configuration lives at `<instance gameDir>/cubiclient/config.json`, not necessarily `~/.minecraft`. Preserve saved positions/binds and one-time `layoutRevision`/`themeRevision` migrations; save interactions or drag completion, not every frame.

More context: `docs/ARCHITECTURE.md` for wiring; `THIRD_PARTY.md` for font licensing and CubicLauncher theme references.
