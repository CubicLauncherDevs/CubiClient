# Dependencias y referencias

La biblioteca `dist/cubiclient-{version}.jar` contiene el código propio del cliente, la pantalla y los assets gráficos generados. El archivo **`dist/replacement/minecraft.jar`** se construye localmente a partir del cliente de Mojang e incluye las clases y recursos de Minecraft con los hooks de Cubi aplicados. Los artefactos descargados y generados están excluidos del repositorio mediante `.gitignore`.

| Dependencia | Versión | Uso / fuente |
| --- | --- | --- |
| Minecraft Java | 1.8.9 | Cliente, manifiesto y assets desde los servidores oficiales de Mojang |
| LaunchWrapper | 1.12 | API usada al preparar los hooks y arranque del formato de biblioteca; `libraries.minecraft.net`. No se incluye ni requiere al ejecutar el jar de reemplazo |
| ASM All | 5.2 | Transformaciones y generación de pantalla al compilar; Maven Central, proyecto [ASM](https://asm.ow2.io/). No se incluye ni requiere al ejecutar el jar de reemplazo |
| LWJGL | 2.9.4-nightly-20150209 | Entrada y OpenGL de Minecraft; `libraries.minecraft.net` |
| Gson | 2.2.4 | Configuración JSON; biblioteca de Minecraft, proyecto [Gson](https://github.com/google/gson) |
| Lato Regular / Bold | Archivos fijados por SHA-1 | Fuente de [Google Fonts](https://github.com/google/fonts/tree/main/ofl/lato), por Łukasz Dziedzic, bajo SIL Open Font License 1.1 |

Los SHA-1 de las dependencias de compilación están fijados en `tools/common.py`. Los hashes de los artefactos originales de Minecraft proceden de su manifiesto. Los hashes comprueban integridad y coincidencia de versión.

La fuente se rasteriza al compilar; el atlas y sus métricas se incluyen en ambos jars. La licencia se incluye en `assets/cubi/ui/OFL-Lato.txt`. Los iconos y la marca del cubo se dibujan con código propio en `UiAssets`; la interfaz toma como referencia el estilo minimalista de clientes PvP y no contiene recursos de Lunar Client o Badlion.

Los nombres de referencia SRG se contrastaron con los mappings **MCP 1.8.9**, publicados en:

```text
https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp/1.8.9/mcp-1.8.9-srg.zip
SHA-1: 7612cfa7e48787ce5aa989c6c513807d18b31a36
```

La compilación no necesita descargar ni redistribuir ese archivo: los accesos específicos están centralizados en `Game189`, `CubiTransformer` y `ScreenGenerator`, y las pruebas los validan contra el cliente original.

Minecraft y sus recursos pertenecen a sus respectivos titulares. CubiClient es un proyecto independiente.
