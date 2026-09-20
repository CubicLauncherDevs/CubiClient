# Verificación y rendimiento

Versión del cliente: **0.0.1**. Los apartados históricos describen etapas de desarrollo, no incrementos de versión.

## Ajuste visual posterior · 2026-09-20

El navegador muestra ahora cuatro tarjetas por página (2 × 2), con vistas previas laterales y dos páginas para los seis módulos. Este cambio también se compila sin ejecutar pruebas; las expectativas anteriores de paginación quedan pendientes de adaptar.

Ping tiene las dimensiones de FPS (70 × 24); Armor Status es vertical (64 × 88), con fondo transparente por defecto y opacidad ajustable desde 0%; Servidor es una fila compacta (136 × 24) con el favicon de la lista vanilla cuando está disponible. Se compila mediante `python3 tools/build.py --replacement`. **Por petición del usuario, se dejan las pruebas para después:** no se ejecutan las suites ni se abre Minecraft. Los resultados del apartado siguiente corresponden al diseño anterior. Quedan pendientes la adaptación de las expectativas de geometría/opacidad y la comprobación gráfica del favicon y de la nueva distribución.

## Módulos básicos y paginación, antes del ajuste visual · 2026-09-20

Se ejecutaron `python3 tools/build.py --replacement --test` y `python3 -m unittest discover -s tools -p 'test_*.py'` con Java 8: **301 comprobaciones Java**, validación del jar de reemplazo, enlace JVM `-Xverify:all` y **7 pruebas Python**.

- `HudTest` comprueba el registro real de seis módulos, coordenadas negativas y límites del mundo, ping ausente frente a cero, conservación de direcciones/puertos/IPv6, estados de desconexión y mundo local, durabilidad acotada y equipo no desgastable.
- Verifica posiciones iniciales sin solapamientos a 320×240, 426×240, 854×480 y 1920×1080; incorporación de módulos ausentes conservando posiciones, escalas, atajos, visibilidad y datos heredados; idempotencia y persistencia después de reiniciar.
- `DeckTest` comprueba las tres páginas, correspondencia entre tarjetas e índices, regreso desde Ajustes/Editor, límites y páginas incompletas. Los nuevos iconos se comprueban en el atlas generado.
- `SelfTest` contrasta los nuevos campos y métodos ofuscados contra el jar original: jugador/UUID, lista de jugadores y ping, servidor, inventario/armadura, daño y máximo de los objetos, renderer e iluminación GUI.

**Pendiente de verificación gráfica:** esta entrega no ha abierto Minecraft. `SmokeTest` está ampliado para recorrer las nuevas páginas, probar sus controles y previsualizaciones, leer ping/servidor local, equipar/dañar/retirar objetos en su mundo aislado y dibujar armadura con batching activado/desactivado comprobando errores GL y estado de profundidad. Esta regresión todavía no se ha ejecutado. Las capturas históricas siguientes no muestran estos cuatro módulos; también queda pendiente verificar los datos en una conexión multijugador real.

## Entrega anterior ejecutada en este entorno

Última verificación de la entrega de rendimiento: **2026-09-19**. Entorno: Linux, OpenJDK **1.8.0_504**, Python 3 y OpenGL NVIDIA GeForce GTX 1650. No es una validación en varios tipos de hardware.

### Compilación y lógica

`python3 tools/build.py --replacement --test`

- Compilación con Java 8 y `-Xlint:all`.
- **236 comprobaciones** sin ventana: ventana móvil de CPS, límites y saturación del buffer, persistencia de opciones visuales, recuperación de JSON, lectura y migración de configuraciones antiguas, ajuste a centro/márgenes, límites de pantalla, argumentos de arranque, bindings del juego/estado gráfico y apertura diferida del menú, además de las regresiones de rendimiento siguientes.
- `DeckTest` verifica navegación, cancelación de captura, regreso del editor a su origen, regiones de clic independientes, límites del deslizador y transformación de coordenadas a 320×240, 426×240, 854×480 y 1920×1080. Con las métricas reales de Cantarell comprueba que el nombre CubiClient quepa en la cabecera y que su marca no invada la hotbar.
- Validación del atlas Cantarell, sus métricas, la licencia incluida y la máscara de borde hueca. La migración del tema se verifica tanto al actualizar como al reiniciar después de personalizar el acento.
- La regresión sin ventana comprueba que el manejador de teclado del código compilado encole la apertura, que no abra directamente una pantalla y que el tick consuma esa solicitud. También verifica activación única, cancelación por cambio de pantalla y ausencia de reaperturas tardías.
- `PerformanceTest`: perfiles y preservación del primer punto de restauración, ediciones externas, caché GUI/Unicode, cajas de partículas interpoladas y 10.000 casos de billboards con esquinas visibles, percentiles, calentamiento, exclusión de frames parciales, exportación y saturación del buffer.
- Validación de las cinco clases objetivo sobre el cliente original **1.8.9** descargado de Mojang; contratos de hooks y rechazo de transformaciones duplicadas.
- Análisis de instrucciones y tipos con ASM `CheckClassAdapter`.
- Comprobación de las firmas de la pantalla generada frente a `GuiScreen`.
- Enlace JVM del jar final bajo `-Xverify:all`, con bibliotecas vanilla y sin ASM/LaunchWrapper, sin inicializar Minecraft ni abrir OpenGL. Comprueba también los stack map frames de la nueva rama de partículas.

### Jar para «Reemplazar Minecraft.jar»

Generado: **`dist/replacement/minecraft.jar`**.

- Verificación del CRC y de la ausencia de entradas duplicadas.
- Conservación byte a byte de las clases y recursos originales, salvo `ave`, `avo`, `aya`, `bec`, `beb` y metadatos de firma/índice.
- Inclusión de los hooks, la pantalla y los assets gráficos preparados al compilar, incluida la licencia de Cantarell.
- Entrada `net.minecraft.client.main.Main` y ausencia de dependencias de ejecución de LaunchWrapper/ASM en Cubi.
- Exclusión de clases de pruebas, herramientas de compilación y el tweaker del archivo final.

### Instalador y procesos de prueba

`python3 -m unittest discover -s tools -p 'test_*.py'`

- Conservación de los argumentos de autenticación, assets, descargas y logging.
- Inclusión de bibliotecas y entrada LaunchWrapper sin modificar el manifiesto base en memoria.
- **7 pruebas Python superadas**, incluyendo conservación de registros, finalización de hijos que ignoran SIGTERM al agotar el tiempo y limpieza al cancelar el controlador, más cálculo de FPS/percentiles desde muestras y rechazo de capturas inconsistentes. Estas pruebas usan procesos Python y no abren Minecraft.

También se ejecutó el instalador en **`build/test-minecraft/`**, generando el descriptor, el jar original y las tres bibliotecas adicionales. Esa carpeta es una instalación de pruebas, separada de la del usuario.

### Integración gráfica real (entrega de rendimiento, anterior a los CPS integrados)

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
- Aplicación de perfiles sobre opciones vanilla, cambio independiente de culling y restauración del vídeo anterior.
- Frustum real bajo una proyección controlada: conserva una partícula estándar visible y descarta una fuera de cámara. Invoca el método parcheado con buffer nulo para verificar que retorna antes de emitir geometría; fuera de la pasada normal el descarte no actúa.
- Dibujo de Rendimiento y Diagnóstico, inicio/parada desde sus botones y exportación JSON de **179 frames** después del calentamiento. Es una muestra funcional parcial de aproximadamente 3 segundos, no un benchmark ni un resultado de mejora.
- Regresión de teclado con eventos LWJGL en partida: apertura diferida, repetición, liberación, cierre, reasignación, atajos de módulo y respeto del chat.

Las acciones de UI se envían programáticamente al hilo del juego. El proceso de prueba aísla sus colas LWJGL de los eventos físicos del escritorio, para evitar que un clic o Esc interfieran con la secuencia. Esto solamente existe en `SmokeTest`, que no se empaqueta en el jar final. Estas pruebas no sustituyen una sesión manual con todos los dispositivos de entrada.

Los resultados actuales están en `run/smoke-replacement/smoke.log`. Se revisaron las imágenes `03-in-world.png`, `07-performance.png` y `09-diagnostic-result.png` de `run/smoke-replacement/screenshots/`; también se generó `08-diagnostics.png`. El JSON funcional está en `run/smoke-replacement/cubiclient/benchmarks/`. El formato de biblioteca dispone de `python3 tools/smoke.py`, pero no se volvió a probar gráficamente en esta entrega.

**Integración posterior de CPS en Keystrokes:** se reconstruyó el jar de reemplazo y pasaron 236 comprobaciones Java, el enlace JVM y las 7 pruebas Python. Se adaptaron navegación y regiones de clic a dos módulos, se comprobó que las etiquetas LMB/RMB y `256 CPS` caben en sus celdas y se verificó la persistencia de posiciones/atajos de `keys` y datos heredados `clicks`. Las dimensiones del teclado siguen siendo 82 × 94. No se abrió Minecraft para este ajuste; las capturas anteriores todavía muestran el widget CPS independiente.

### Historial anterior a la entrega de rendimiento

**Etapa de corrección del teclado:** se añadió una regresión que introduce eventos en la cola de LWJGL y deja que `Minecraft.runTick` los procese: abrir en partida, mantener/soltar/cerrar, reasignar la tecla, usar atajos de módulos y respetar el chat. El intento gráfico se interrumpió y la instancia de pruebas que quedó abierta se cerró. No se volvió a abrir una ventana después de esa interrupción. La compilación, las 92 comprobaciones sin ventana, las 4 pruebas Python y la validación del jar final sí pasaron; queda pendiente completar la nueva regresión gráfica.

**Etapa del tema Cubic Oscuro:** el tema se compiló y empaquetó con Java 8. Pasaron las 104 comprobaciones Java en modo headless, las 4 pruebas Python y la validación del jar completo. No se abrió Minecraft para esta actualización; la apariencia de Cantarell, los controles nuevos y los bordes del HUD requieren todavía revisión dentro del juego. Las capturas anteriores corresponden al diseño anterior.

**Etapa de separación de vistas (0.0.1):** la interfaz separó Módulos, Apariencia, Ajustes y Editor y mantuvo el nombre completo CubiClient. Pasaron entonces 154 comprobaciones Java sin ventana, 4 pruebas Python y la validación del jar final. La revisión gráfica quedó pendiente en esa etapa; se completó para el jar de reemplazo en la entrega de rendimiento descrita arriba.

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
- Caché de resolución escalada con invalidación completa, ruta rápida de HUD vacío y muestreo condicionado a módulos activos.
- Geometría del atlas agrupada por widget y culling conservador de partículas estándar, ambos desactivables desde Rendimiento.
- Captura opcional de fotogramas y etapas con memoria acotada y exportación por acción.

La métrica interna **`HUD / N us`**, mostrada en el log del smoke test, calcula la media del tiempo transcurrido al dibujar los widgets durante la última ventana de 20 ticks con muestras. Puede incluir esperas/desplanificación; no es CPU exclusiva ni incluye todo el trabajo GPU o el renderizado del mundo.

Se comprobó que el HUD puede medirse y dibujarse dentro del mundo. No se ha hecho una comparación A/B controlada de FPS contra vanilla; una muestra de arranque con generación de chunks no es un benchmark de rendimiento.

### Medición recomendada para continuar

1. Usa el mismo mundo, resolución, distancia de renderizado y posición de cámara.
2. Espera a que termine la generación de chunks y el calentamiento de la JVM.
3. Compara optimizaciones activadas/desactivadas con los mismos ajustes. Para medir HUD vacío, desactiva los seis módulos **y la marca**; conserva F1/F3 iguales.
4. Registra tiempos de fotograma durante al menos un minuto en cada caso.
5. Repite con la misma configuración en vanilla para separar el coste de Cubi del del motor.

Consulta [PERFORMANCE.md](PERFORMANCE.md) para perfiles, semántica de las capturas, limitaciones de la muestra interna y comparación entre clientes con una herramienta externa común.

## Alcance pendiente de validar

- Selección/inicio desde la interfaz de Prism y autenticación con una cuenta real. Se verificó el arranque directo del archivo final y se contrastó el funcionamiento de `installCustomJar_internal` en el código de Prism; no se automatizó su interfaz.
- Sesiones PvP en servidores y pruebas prolongadas de entrada física, redimensionado y fullscreen.
- Windows y macOS: los scripts contemplan sus rutas, pero la ejecución verificada fue en Linux.
- Combinación con OptiFine, Forge u otros transformadores.

La versión objetivo es exactamente **Minecraft Java 1.8.9 vanilla**.
