# Arquitectura de CubiClient

Versión del cliente: **0.0.1**. Las revisiones internas de configuración y tema son independientes de este número.

## Arranque

### Jar de reemplazo para Prism

```text
Compilación: Mojang 1.8.9 + hooks preaplicados + Cubi + GuiScreen generada
                                      ↓
                       dist/replacement/minecraft.jar
                                      ↓
Prism → net.minecraft.client.main.Main → Hooks → CubiClient
```

`tools/build.py --replacement` compila las utilidades de `src/build/java/`. `PrepareClasses` aplica `CubiTransformer` a las cinco clases de `CubiTransformer.TARGETS` y valida el bytecode. `ScreenGenerator` genera `cubi.generated.ControlScreen` durante la compilación.

`tools/replacement.py` combina las clases preparadas, el resto del jar original y las clases de Cubi. Renueva el manifiesto y retira las firmas/índices que ya no describen el contenido modificado. Verifica todas las entradas originales: solamente pueden cambiar las cinco clases objetivo y esos metadatos. El resultado se publica mediante un reemplazo atómico tras validar su contenido.

El jar final conserva la entrada estándar de Minecraft. Las utilidades de compilación, las pruebas, LaunchWrapper y ASM quedan fuera del archivo; se comprueba también que las clases de Cubi no conserven referencias a esas dos dependencias. Prism aporta las bibliotecas normales de 1.8.9 y los assets.

### Biblioteca y versión local

```text
Launcher → LaunchWrapper → CubiTweaker → Minecraft 1.8.9
                                  └─ CubiTransformer
                                        └─ Hooks → CubiClient
```

El descriptor de la versión conserva assets, bibliotecas, configuración de logging y argumentos de sesión del manifiesto oficial. La clase principal cambia a `net.minecraft.launchwrapper.Launch`, con el argumento `--tweakClass dev.cubi.launch.CubiTweaker`.

En este formato alternativo, el transformador se ejecuta al cargar las clases. Ambos formatos comparten exactamente los mismos puntos de integración:

| Clase original | Mapping MCP/SRG de referencia | Integración |
| --- | --- | --- |
| `ave` / Minecraft | `func_71407_l` / `s()V` | Actualización por tick; captura de eventos de `Mouse.next()` |
| `ave` / Minecraft | `func_152348_aa` / `Z()V` | Teclado, filtrando repetición y eventos ya consumidos por el menú |
| `avo` / GuiIngame | `func_175180_a` / `a(F)V` | Dibujo del HUD al final del overlay vanilla |
| `aya` / GuiMainMenu | `func_73863_a` / `a(IIF)V` | Marca de Cubi en el menú principal |
| `ave` / Minecraft | `av()V` | Captura opcional del fotograma y etapas: tick, render, presentación y limitador |
| `bec` / EffectRenderer | `a(Lpk;F)V` | Delimita la pasada normal de partículas elegible para culling |
| `beb` / EntityFX | `a(Lbfd;Lpk;FFFFFF)V` | Retorno temprano para billboards estándar totalmente fuera de cámara |

Las firmas se contrastan con el `.jar` oficial en `SelfTest`. El transformador conserva las formas de la pila y los stack frames originales; recalcula máximos y emite un frame explícito para la rama nueva al entrar en `beb`. Si falta un punto de integración esperado o ya hay hooks, falla. `--replacement --test` también enlaza las clases finales bajo `-Xverify:all` con las bibliotecas vanilla, sin inicializar el juego ni abrir una ventana.

## Adaptador de Minecraft

`Game189` centraliza nombres ofuscados, campos, constructores y `MethodHandle`s. Se resuelven una sola vez al inicializar Cubi; los bindings de partículas se inicializan de forma diferida y aislada. Los caminos de dibujo invocan firmas primitivas con `invokeExact`, evitando buscar métodos o construir listas de argumentos por fotograma. `ResolutionCache` conserva las dimensiones escaladas hasta que cambien resolución, escala GUI o Unicode efectivo.

`ScreenFactory` instancia la subclase real de `GuiScreen` (`axu`) que preparó `ScreenGenerator` al compilar. Sus callbacks apuntan a `ControlDeck`. Esto permite usar el flujo de entrada, cursor y cambio de pantalla de Minecraft sin mantener una copia de sus fuentes ni generar bytecode durante el juego.

Con el jar de reemplazo, `Game189` usa el classloader que cargó Cubi y Minecraft. En el formato de biblioteca, `CubiTweaker` proporciona explícitamente el classloader de LaunchWrapper. La pantalla está en `cubi.generated`, fuera de la exclusión `dev.cubi.*`, para que en ese formato la cargue el mismo loader que `GuiScreen`.

`Ink` usa el atlas de `UiAtlas` para tipografía, iconos y superficies redondeadas. Los rectángulos simples usan `Gui.drawRect`. La selección de texturas, color, blending y alpha pasa por handles cacheados de `GlStateManager`, para mantener sincronizada su caché. Las transformaciones y los bloques de dibujo se equilibran con `try/finally`; al terminar se restablecen alpha, blending y color.

Con `hudBatching`, cada widget agrupa su geometría del atlas entre la aplicación y retirada de su matriz. Dentro de `glBegin` solamente cambia color y emite vértices/coordenadas; cualquier rectángulo vanilla fuerza un cierre previo. La marca usa un bloque independiente. Los controles del menú mantienen su dibujo habitual.

`UiAssets` rasteriza Cantarell Regular/Bold a 36 píxeles y los iconos originales de Cubi durante la compilación. Comprueba la familia de la fuente y que los glifos quepan en sus celdas. Produce `atlas.png` de 2048 × 1024, métricas binarias y una copia de la licencia OFL. El runtime carga una sola textura (8 MiB RGBA), reutilizada durante toda la sesión. No genera glifos, imágenes o geometría curva por fotograma. Los fondos y los bordes usan máscaras de nueve secciones; la máscara del borde tiene un centro transparente para no rellenar de nuevo los widgets translúcidos. Las animaciones usan tiempo transcurrido, no incrementos por FPS.

## Ciclo de vida y módulos

Todo el estado del cliente se usa desde el hilo principal de Minecraft. Hay tres fases explícitas:

1. **Entrada:** eventos de teclado y ratón.
2. **Tick:** caducidad de CPS, actualización de etiquetas, muestreo de FPS y `HudModule.tick`.
3. **Dibujo:** recorrido de un array estable de módulos activados y `HudModule.paint`.

La apertura del menú se difiere mediante `MenuActivation`: `CubiClient.key` registra la solicitud y el hook de fin de tick la consume. En vanilla 1.8.9, `runTick` llama primero a `dispatchKeypresses` y después reenvía esa misma pulsación a la pantalla que esté abierta. Abrirla dentro del primer callback provoca que el nuevo menú reciba la tecla de cierre inmediatamente. La solicitud diferida evita esa doble acción y comprueba que la pantalla original siga siendo la misma para no reemplazar chat, inventario u otra interfaz abierta mientras tanto. Los atajos de módulos siguen siendo inmediatos; las repeticiones de las teclas de cierre/captura se ignoran.

No hay un bus de eventos reflectivo ni objetos de evento creados en cada fotograma. No hay trabajadores en segundo plano en producción. La prueba gráfica sí usa un hilo de automatización, que envía acciones a la cola del hilo principal de Minecraft.

### Añadir un widget

1. Crea una subclase de `HudModule` con ID estable, título, descripción y dimensiones lógicas.
2. Implementa `paint(CubiClient, Ink, boolean preview)`. Usa datos ya preparados cuando sea posible.
3. Usa `tick` si necesitas muestrear información del juego. Reserva el dibujo para lo visual.
4. Registra el módulo en el array de `ModuleRegistry`.
5. Amplía la lista del panel si superas los dos espacios actuales (FPS y Keystrokes con CPS).

El contrato común se encarga de escala, posición, persistencia y representación en el editor. Si un nuevo módulo necesita acceder al juego, añade el acceso cacheado a `Game189` y su comprobación al test de mappings.

`ModuleRegistry.frames` y `ModuleRegistry.keys` identifican los consumidores sin depender del índice del array. Keystrokes muestra los CPS bajo LMB/RMB: los eventos se capturan mientras está activo y las etiquetas se preparan en el tick. Al desactivarlo, los buffers se vacían. Mantiene su ID `keys` y dimensiones 82 × 94; los ajustes heredados `clicks` se conservan en el JSON, sin registrar un widget independiente.

## Identidad visual

- **Referencia:** tema Oscuro de CubicLauncher, revisión `c7b6ecb408472964158a3757b7fd7cabb4af8e42`.
- **Fondos:** `#0C0C0C`, `#0F1010` y tarjetas `#16161A`.
- **Bordes:** normal `#242424`, hover `#383838` y selección `#777777`.
- **Texto:** principal `#D8D8D8` y secundario `#909090`.
- **Acento:** blanco `#FFFFFF` por defecto; azul, lavanda y melocotón opcionales.
- **Marca:** cubo isométrico de trazo fino y nombre completo **CubiClient**, centralizado en `ClientIdentity`.
- **Jerarquía:** encabezados en mayúsculas con espaciado entre letras, separadores finos y controles alineados.
- **Tipografía:** Cantarell Regular y Bold, suavizada y precalculada.
- **Controles:** botones blancos o secundarios con borde, casillas de verificación y deslizadores con tirador cuadrado.
- **Efectos:** transparencia regulable, radios discretos, sombra suave y transiciones de teclas/casillas.

`Theme` centraliza la paleta y las dimensiones de los bordes/radios. `Ink` adapta esos valores a primitivas compartidas por la HUD, el menú y el editor. No se consulta la configuración de CubicLauncher durante el juego: es un tema integrado en CubiClient.

El panel usa un lienzo lógico **560 × 362** que se adapta a la pantalla. Las vistas previas muestran valores de ejemplo para poder valorar el diseño incluso fuera de un mundo. Los widgets usan su propia escala; el editor opera en coordenadas GUI de Minecraft. `HudPlacement` realiza el ajuste a centro y márgenes de 12 píxeles y mantiene la posición dentro de pantalla. Las guías aparecen al arrastrar; Shift desactiva el magnetismo.

### Navegación y distribución

- `DeckState` gestiona Módulos, Apariencia, Rendimiento, Diagnóstico, Ajustes y Editor, la selección y el foco de captura de teclas. No depende de Minecraft ni de OpenGL.
- `DeckLayout` comparte dimensiones y regiones de clic entre dibujo, interacción y pruebas. La activación de una tarjeta tiene un área distinta de su engranaje.
- `ControlDeck` dibuja y despacha las acciones de la vista activa. Los ajustes del módulo van en una columna junto a su previsualización; los globales pertenecen a Apariencia.
- Esc cancela primero una captura, vuelve desde Ajustes a Módulos o desde Editor a su origen. La tecla del menú cierra toda la interfaz cuando no se está capturando un atajo.
- Al navegar se terminan los arrastres y se guardan los cambios pendientes. Las posiciones y opciones existentes no se reinician al cambiar de vista.

La marca del HUD mide el nombre completo con las métricas del atlas. Si no cabe junto a la hotbar, `HudPlacement.watermarkY` la coloca por encima; con espacio suficiente conserva la esquina inferior izquierda.

## Persistencia y fallos

`ClientConfig` usa esquema 1 y añade campos compatibles con las configuraciones anteriores. Valida números finitos, posiciones, escala, opacidad, acento y códigos de tecla. La revisión de distribución 2 adapta las coordenadas guardadas a las dimensiones compactas una sola vez. Guarda a un temporal antes de reemplazar el JSON; si el sistema no permite movimientos atómicos, usa un reemplazo normal. Las configuraciones inválidas se copian antes de recuperar valores por defecto.

`themeRevision` permite aplicar el acento blanco del nuevo tema una sola vez. La migración no modifica la distribución ni las opciones de los módulos y marca la configuración como pendiente de guardar; las personalizaciones posteriores de acento se respetan.

Las excepciones en los hooks se registran una vez y desactivan la capa de Cubi para evitar un bucle de errores por fotograma. Un fallo durante la pantalla personalizada intenta volver a la pantalla anterior. Los errores de escritura aparecen también en el estado del panel.

## Rendimiento

`PerformanceSettings` persiste las optimizaciones propias y el punto de restauración. `VideoSettings` es una instantánea de las opciones gestionadas; Minecraft sigue siendo su autoridad y el arranque no reaplica perfiles. `PerformancePanel` prepara etiquetas al entrar o interactuar y delega las aplicaciones agrupadas a `Game189`.

`ParticleVisibility` hace la prueba pura de caja contra planos. `Game189.ParticleAccess` conserva handles de posición/escala, clasifica por `ClassValue` los tipos cuyo renderer sigue siendo el base y captura el frustum una vez por pasada elegible. No afecta a creación ni ticks de partículas, ni usa trabajadores. Los fallos desactivan este subsistema sin desactivar el HUD.

`FrameCapture` reserva almacenamiento acotado al iniciar una captura opcional. Los hooks del bucle no inicializan Cubi ni escriben archivos. Los percentiles se calculan desde acciones del panel; la exportación explícita usa un temporal y produce un JSON con muestras y contexto. Véase [PERFORMANCE.md](PERFORMANCE.md) para la semántica temporal y el protocolo de comparación.
