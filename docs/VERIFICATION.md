# Verificación y rendimiento

## Ejecutado en este entorno

Entorno: Linux, OpenJDK **1.8.0_504**, Python 3 y sesión gráfica OpenGL disponible.

### Compilación y lógica

`python3 tools/build.py --replacement --test`

- Compilación con Java 8 y `-Xlint:all`.
- **92 comprobaciones**: ventana móvil de CPS, límites y saturación del buffer, persistencia de opciones visuales, recuperación de JSON, lectura de configuraciones antiguas, ajuste a centro/márgenes, límites de pantalla, argumentos de arranque, bindings del juego/estado gráfico y apertura diferida del menú.
- La regresión sin ventana comprueba que el manejador de teclado del código compilado encole la apertura, que no abra directamente una pantalla y que el tick consuma esa solicitud. También verifica activación única, cancelación por cambio de pantalla y ausencia de reaperturas tardías.
- Validación de los tres transformadores sobre el cliente original **1.8.9** descargado de Mojang.
- Análisis de instrucciones y tipos con ASM `CheckClassAdapter`.
- Comprobación de las firmas de la pantalla generada frente a `GuiScreen`.

### Jar para «Reemplazar Minecraft.jar»

Generado: **`dist/replacement/minecraft.jar`**.

- Verificación del CRC y de la ausencia de entradas duplicadas.
- Conservación byte a byte de las clases y recursos originales, salvo las tres clases parcheadas y metadatos de firma/índice.
- Inclusión de los hooks, la pantalla y los assets gráficos preparados al compilar, incluida la licencia de Lato.
- Entrada `net.minecraft.client.main.Main` y ausencia de dependencias de ejecución de LaunchWrapper/ASM en Cubi.
- Exclusión de clases de pruebas, herramientas de compilación y el tweaker del archivo final.

### Instalador y procesos de prueba

`python3 -m unittest discover -s tools -p 'test_*.py'`

- Conservación de los argumentos de autenticación, assets, descargas y logging.
- Inclusión de bibliotecas y entrada LaunchWrapper sin modificar el manifiesto base en memoria.
- **4 pruebas Python superadas**, incluyendo conservación de registros, finalización de hijos que ignoran SIGTERM al agotar el tiempo y limpieza al cancelar el controlador. Estas pruebas usan procesos Python y no abren Minecraft.

También se ejecutó el instalador en **`build/test-minecraft/`**, generando el descriptor, el jar original y las tres bibliotecas adicionales. Esa carpeta es una instalación de pruebas, separada de la del usuario.

### Integración gráfica real (última ejecución completa: 0.2)

`python3 tools/smoke.py --replacement`

Se arrancó el **jar de reemplazo final** mediante la entrada estándar de Minecraft, con las bibliotecas vanilla. La prueba comprueba que tanto Minecraft como Cubi se cargan desde ese archivo y que LaunchWrapper/ASM no están disponibles. Se verificaron:

- Inicialización de Cubi y apertura de la subclase real de `GuiScreen`.
- Dibujo de Control Deck, widgets y editor.
- Activación de módulos, controles de escala y captura/limpieza de atajos.
- Carga y dibujo del atlas, controles de fondo y sombra, deslizador de opacidad, selección de acento y persistencia de esos ajustes.
- Ausencia de errores OpenGL tras dibujar e interactuar con la interfaz.
- Correspondencia de WASD con la configuración real de Minecraft.
- Desplazamiento de widgets y regreso al menú anterior.
- Entrada a un **mundo plano local** y dibujo del HUD sobre el juego.
- Creación de capturas y cierre normal del cliente/servidor integrado.

Las acciones de UI se envían programáticamente al hilo del juego. El proceso de prueba aísla sus colas LWJGL de los eventos físicos del escritorio, para evitar que un clic o Esc interfieran con la secuencia. Esto solamente existe en `SmokeTest`, que no se empaqueta en el jar final. Estas pruebas no sustituyen una sesión manual con todos los dispositivos de entrada.

Los resultados y capturas de la interfaz 0.2 están en `run/smoke-replacement/`. El formato de biblioteca también dispone de `python3 tools/smoke.py`; su última ejecución documentada antes de este rediseño comprobó la adaptación del classloader y la pantalla pregenerada.

**Estado de 0.2.1:** se añadió una regresión que introduce eventos en la cola de LWJGL y deja que `Minecraft.runTick` los procese: abrir en partida, mantener/soltar/cerrar, reasignar la tecla, usar atajos de módulos y respetar el chat. El intento gráfico se interrumpió y la instancia de pruebas que quedó abierta se cerró. No se volvió a abrir una ventana después de esa interrupción. La compilación, las 92 comprobaciones sin ventana, las 4 pruebas Python y la validación del jar final sí pasaron; queda pendiente completar la nueva regresión gráfica.

Estas pruebas descargan el índice de assets pero omiten los archivos de audio: por eso puede haber avisos de sonidos ausentes. La integración antigua de Twitch de Minecraft también puede registrar un error de inicialización en Linux; las pruebas de Cubi y el mundo local se completaron.

## Qué significa «ligero» aquí

Implementado:

- Separación entre preparación de datos y dibujo.
- Etiquetas de FPS/CPS actualizadas solamente cuando cambia su valor.
- Buffers de CPS de 256 eventos por botón.
- Captura de los eventos reales del ratón; no se muestrea una vez cada tick para contar clics.
- Referencias al juego y handles de métodos cacheados.
- Recorrido simple de módulos activos.
- Una textura compartida con fuentes, iconos y esquinas precalculadas. La fuente se obtiene al compilar y queda rasterizada dentro del jar; el juego no la descarga.
- Sin desenfoque, generación de glifos, descargas ni escritura de configuración en el bucle de dibujo del HUD.
- Guardado por interacción/cierre; un arrastre se guarda al soltar.

La métrica interna **`HUD / N us`**, mostrada en el log del smoke test, calcula la media del tiempo de CPU empleado en dibujar los widgets durante la última ventana de 20 ticks con muestras. Esta medición no incluye el trabajo completo de la GPU ni el renderizado del mundo.

Se comprobó que el HUD puede medirse y dibujarse dentro del mundo. No se ha hecho una comparación A/B controlada de FPS contra vanilla; una muestra de arranque con generación de chunks no es un benchmark de rendimiento.

### Medición recomendada para continuar

1. Usa el mismo mundo, resolución, distancia de renderizado y posición de cámara.
2. Espera a que termine la generación de chunks y el calentamiento de la JVM.
3. Compara Cubi con los tres módulos desactivados frente a activados.
4. Registra tiempos de fotograma durante al menos un minuto en cada caso.
5. Repite con la misma configuración en vanilla para separar el coste de Cubi del del motor.

## Alcance pendiente de validar

- Selección/inicio desde la interfaz de Prism y autenticación con una cuenta real. Se verificó el arranque directo del archivo final y se contrastó el funcionamiento de `installCustomJar_internal` en el código de Prism; no se automatizó su interfaz.
- Sesiones PvP en servidores y pruebas prolongadas de entrada física, redimensionado y fullscreen.
- Windows y macOS: los scripts contemplan sus rutas, pero la ejecución verificada fue en Linux.
- Combinación con OptiFine, Forge u otros transformadores.

La versión objetivo es exactamente **Minecraft Java 1.8.9 vanilla**.
