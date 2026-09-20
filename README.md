# CubiClient · 0.1.0

**Cliente PvP para Minecraft Java 1.8.9 con HUD minimalista, tipografía suavizada y widgets personalizables.**

Preparado para **Prism Launcher → Reemplazar Minecraft.jar**, sobre una instancia de **Minecraft 1.8.9 con Java 8**. El archivo listo para seleccionar es **`dist/replacement/minecraft.jar`**.

### Corrección de teclado en 0.1.0

La apertura con Shift derecho se aplica al terminar el procesamiento de entrada del tick. Así, Minecraft no reenvía la pulsación de apertura al menú recién creado como si fuera una orden de cierre. Mantener la tecla pulsada tampoco cierra el panel por repetición. Si otra pantalla se abre mientras tanto, se cancela la solicitud pendiente.

## Lo que incluye

| Componente | Función |
| --- | --- |
| **Menú CubiClient** | Tarjetas de módulos, ejemplos visuales, interruptores animados, atajos y ajustes de apariencia |
| **FPS** | Indicador compacto de FPS sobre fondo translúcido opcional |
| **CPS** | Contador izquierdo/derecho en una ventana móvil de un segundo |
| **Keystrokes** | Teclas independientes con transición de color; respeta los controles asignados en Minecraft |
| **Editor del HUD** | Arrastrar, alinear con centro y bordes, ajuste fino, escala y restablecimiento |
| **Apariencia** | Fondo, sombra y opacidad por módulo; cuatro acentos globales y firma Cubi opcional |
| **Configuración** | JSON versionado, escritura atómica y copia de recuperación si está dañado |
| **Integración** | Marca en el menú principal, título de ventana y pantalla real de Minecraft |

La base mantiene el renderizado del mundo de Minecraft 1.8.9. Las optimizaciones implementadas se centran en que la capa de Cubi tenga poco coste: consultas cacheadas, eventos sin asignaciones auxiliares, buffers de tamaño fijo y trabajo por tick separado del dibujo. No incluye OptiFine ni parches de optimización de chunks.

## Requisitos

- **JDK 8** para compilar (`java` y `javac`). Configura `JAVA_HOME` si tienes varias versiones.
- **Python 3.9 o superior**, sin paquetes adicionales.
- Internet para la primera descarga de dependencias y recursos.
- **Prism Launcher** con una instancia de Minecraft 1.8.9. Prism gestiona la cuenta, Java, las bibliotecas originales y los recursos del juego.

## Compilar

Desde esta carpeta:

```bash
python3 tools/build.py --replacement --test
```

En Windows puedes usar `py -3` en lugar de `python3`.

El resultado es:

```text
dist/replacement/minecraft.jar
```

El script descarga versiones fijas de las dependencias, la fuente Lato y el cliente original de Mojang, comprueba los hashes y aplica los hooks. Genera la pantalla y un atlas gráfico compartido de fuentes, iconos y esquinas redondeadas durante la compilación. Valida que se conserven las demás clases y recursos. El jar de reemplazo **no necesita LaunchWrapper ni ASM durante el juego**. No hace falta configurar un workspace MCP antiguo ni instalar Gradle.

## Instalar en Prism Launcher

1. Crea una instancia de **Minecraft 1.8.9** en Prism, o selecciona una instancia vanilla de esa versión.
2. Con la instancia detenida, entra en **Editar → Versión**.
3. Pulsa **Reemplazar Minecraft.jar** (*Replace Minecraft.jar*).
4. Selecciona **`dist/replacement/minecraft.jar`** de este proyecto.
5. En **Configuración → Java** de la instancia, selecciona **Java 8**. Activa la opción de configuración específica de la instancia si hace falta.
6. Inicia el juego. Prism descargará los recursos y bibliotecas normales de Minecraft que falten.
7. Pulsa **Shift derecho** para abrir el menú Cubi, tanto en el menú principal como dentro de un mundo.

**Selecciona el archivo de `dist/replacement/`.** El archivo más pequeño `dist/cubiclient-{version}.jar` es la biblioteca del formato de instalación anterior y no es un Minecraft.jar completo.

Prism copia el archivo seleccionado dentro de la instancia. Después de recompilar una actualización, vuelve a usar **Reemplazar Minecraft.jar** para que Prism copie la versión nueva. La configuración se guarda en `cubiclient/config.json` dentro de la carpeta de Minecraft de esa instancia.

Para este formato se utiliza la entrada estándar `net.minecraft.client.main.Main`: no hay que añadir argumentos `--tweakClass` ni ejecutar el instalador de versiones locales.

### Formato de versión local

El proyecto también conserva `tools/install.py` para el formato `versions/<id>/<id>.json` de otros launchers. Usa `dist/cubiclient-{version}.jar` como biblioteca con LaunchWrapper. Se compila con `python3 tools/build.py --test` y se instala con `python3 tools/install.py --minecraft-dir "/ruta/a/.minecraft"`. Ese script no configura instancias de Prism.


## Controles

| Acción | Control |
| --- | --- |
| Abrir panel | **Shift derecho**, configurable haciendo clic en «RSHIFT / Abrir menú» |
| Seleccionar módulo | Clic en su tarjeta |
| Activar/desactivar | Interruptor de la tarjeta |
| Asignar atajo | Selecciona la tarjeta y pulsa «Tecla: …» |
| Quitar atajo | Backspace o Supr durante la captura |
| Cancelar captura | Esc |
| Mover widget | «Editar HUD», después arrastrar |
| Ajustar con precisión | Shift izquierdo durante arrastre o flechas |
| Desplazar selección | Flechas: 4 píxeles; con Shift: 1 píxel |
| Cambiar selección | Tab en el editor |
| Cambiar escala | `+` / `-` en el editor o botones del panel, entre 75% y 200% |
| Cambiar opacidad | Deslizador del módulo seleccionado, entre 10% y 85% |
| Cambiar fondo/sombra | Interruptores del módulo seleccionado |
| Cambiar acento | Muestras de menta, azul, lavanda y melocotón |
| Mostrar/ocultar marca | «Firma Cubi», al pie del menú |
| Guardar y volver | «Listo» o Esc |

Los atajos de módulo se aplican mientras juegas, con las pantallas cerradas. El HUD se oculta al abrir interfaces, con F1 y con F3 para no tapar el juego ni el depurador. El editor puede mostrar también los módulos desactivados para que puedas colocarlos antes de activarlos.

### Configuración

Se guarda en el **directorio de juego de la instalación**:

```text
cubiclient/config.json
```

Cada módulo conserva `enabled`, `key`, `x`, `y`, `scale`, `background`, `shadow` y `opacity`. `accent` y `watermark` son ajustes globales. Las posiciones están normalizadas dentro del espacio disponible de la pantalla. Las configuraciones 0.1 se migran una vez para conservar aproximadamente la esquina superior izquierda de cada widget al reducir sus dimensiones; los atajos y las escalas se conservan. «Restablecer» aplica la distribución compacta nueva.

Los cambios se guardan al interactuar con los controles o cerrar el editor; los arrastres del HUD y del deslizador no escriben en cada fotograma. Si el JSON está dañado, se conserva una copia `config.json.invalid-<fecha>`.

## Verificaciones

```bash
# Jar de reemplazo + comprobaciones sobre el Minecraft 1.8.9 original
python3 tools/build.py --replacement --test

# Descriptor del launcher y limpieza de procesos de prueba (sin ventana)
python3 -m unittest discover -s tools -p 'test_*.py'

# Opcional: prueba el jar final con arranque directo, menú, editor y mundo local
python3 tools/smoke.py --replacement
```

La prueba gráfica necesita una sesión gráfica y OpenGL. Con `--replacement`, carga Cubi y Minecraft exclusivamente desde el jar final, sin `build/classes`, LaunchWrapper ni ASM en el classpath. Usa `run/smoke-replacement/`, crea un mundo plano de pruebas y se cierra sola. Reescribe sus opciones gráficas de prueba, aísla los eventos físicos de entrada dentro de ese proceso y omite la descarga de audio. Los artefactos quedan en:

```text
run/smoke-replacement/smoke.log
run/smoke-replacement/screenshots/01-control-deck.png
run/smoke-replacement/screenshots/02-layout-editor.png
run/smoke-replacement/screenshots/03-in-world.png
```

El registro se escribe mientras la prueba está en marcha. Si se cancela o agota el tiempo, el script termina y recoge su proceso de juego; en Linux también configura el cierre del hijo si el proceso controlador desaparece. La regresión de teclado añadida en 0.2.1 usa eventos LWJGL dentro de un mundo. Su ejecución gráfica quedó pendiente tras interrumpirse la prueba; las comprobaciones sin ventana de esta versión sí se completaron.

Consulta [verificación y rendimiento](docs/VERIFICATION.md) para el alcance real de las comprobaciones y [arquitectura](docs/ARCHITECTURE.md) para ampliar el cliente.

## Estructura

```text
src/main/java/dev/cubi/
  launch/       Entrada de la versión y transformaciones de bytecode
  bridge/       Adaptador cacheado de Minecraft 1.8.9 e instanciación de GuiScreen
  core/         Ciclo de vida, puntos de entrada, métricas y ventana de CPS
  config/       Persistencia y validación
  module/       Contrato de HUD, registro y los tres módulos iniciales
  ui/           Paleta, primitivas gráficas, Control Deck y editor
src/build/java/ Generación de GuiScreen, atlas tipográfico/gráfico y hooks al compilar
src/test/java/  Pruebas de lógica, bytecode e integración gráfica
tools/         Compilación, instalación y pruebas sin dependencias de Python
```

## Si algo falla

- **`javac` no existe o no es 1.8:** configura `JAVA_HOME` a un **JDK 8**, no solamente un JRE.
- **Prism abre Minecraft sin Cubi:** confirma que seleccionaste `dist/replacement/minecraft.jar` en una instancia 1.8.9 y que el componente del jar personalizado está activo.
- **Un cambio no aparece después de compilar:** vuelve a seleccionar el jar desde «Reemplazar Minecraft.jar»; Prism utiliza su copia local.
- **No aparece el HUD:** verifica que los módulos estén activados y que no estén abiertas otra pantalla ni F3. Busca `[Cubi]` en `logs/latest.log` del juego.
- **Modificaciones adicionales al juego:** esta base está validada contra **vanilla 1.8.9**. Combinar transformadores de Forge, OptiFine u otros clientes requiere trabajo de compatibilidad.

Dependencias y fuentes: [THIRD_PARTY.md](THIRD_PARTY.md).
