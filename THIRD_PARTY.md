# Dependencias y referencias

La biblioteca `dist/cubiclient-0.0.1.jar` contiene el código propio del cliente, la pantalla y los assets gráficos generados. El archivo **`dist/replacement/minecraft.jar`** se construye localmente a partir del cliente de Mojang e incluye las clases y recursos de Minecraft con los hooks de Cubi aplicados. Los artefactos descargados y generados están excluidos del repositorio mediante `.gitignore`.

| Dependencia | Versión | Uso / fuente |
| --- | --- | --- |
| Minecraft Java | 1.8.9 | Cliente, manifiesto y assets desde los servidores oficiales de Mojang |
| LaunchWrapper | 1.12 | API usada al preparar los hooks y arranque del formato de biblioteca; `libraries.minecraft.net`. No se incluye ni requiere al ejecutar el jar de reemplazo |
| ASM All | 5.2 | Transformaciones y generación de pantalla al compilar; Maven Central, proyecto [ASM](https://asm.ow2.io/). No se incluye ni requiere al ejecutar el jar de reemplazo |
| LWJGL | 2.9.4-nightly-20150209 | Entrada y OpenGL de Minecraft; `libraries.minecraft.net` |
| Gson | 2.2.4 | Configuración JSON; biblioteca de Minecraft, proyecto [Gson](https://github.com/google/gson) |
| Cantarell Regular / Bold | Archivos TTF fijados por SHA-1 | Fuente de [Google Fonts](https://github.com/google/fonts/tree/main/ofl/cantarell), Copyright 2009 The Cantarell Project Authors, bajo SIL Open Font License 1.1 |

Los SHA-1 de las dependencias de compilación están fijados en `tools/common.py`. Los hashes de los artefactos originales de Minecraft proceden de su manifiesto. Los hashes comprueban integridad y coincidencia de versión.

La fuente se rasteriza al compilar; el atlas y sus métricas se incluyen en ambos jars. La licencia se incluye en `assets/cubi/ui/OFL-Cantarell.txt`. Se utiliza la distribución estática TTF de Cantarell para generar el atlas con Java 8; no se cargan las fuentes WOFF2 del launcher en el juego. Los iconos, la marca de Cubi y las máscaras de controles se dibujan con código propio en `UiAssets`.

## Referencia visual de CubicLauncher

La paleta y el estilo de los controles toman como referencia [CubicLauncher](https://github.com/CubicLauncherDevs/CubicLauncher), revisión `c7b6ecb408472964158a3757b7fd7cabb4af8e42`:

- `static/themes/dark/dark.json`: paleta del tema Oscuro, tipografía y radios.
- `src/styles/shared/components.css`: botones primarios/secundarios y etiquetas.
- `src/lib/components/settings/controls.css`: casillas de verificación y deslizadores.

El repositorio de referencia se publica bajo GPL-3.0-only. CubiClient implementa sus propios componentes Java/OpenGL y conserva su propia marca; no incorpora el frontend Svelte ni los binarios de CubicLauncher.

Los nombres de referencia SRG se contrastaron con los mappings **MCP 1.8.9**, publicados en:

```text
https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp/1.8.9/mcp-1.8.9-srg.zip
SHA-1: 7612cfa7e48787ce5aa989c6c513807d18b31a36
```

La compilación no necesita descargar ni redistribuir ese archivo: los accesos específicos están centralizados en `Game189`, `CubiTransformer` y `ScreenGenerator`, y las pruebas los validan contra el cliente original.

Minecraft y sus recursos pertenecen a sus respectivos titulares. CubiClient es un proyecto independiente.
