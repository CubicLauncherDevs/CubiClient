# Rendimiento · CubiClient 0.0.1

Abre **Shift derecho → Rendimiento**. Los botones de opciones avanzan al siguiente valor; al llegar al máximo vuelven al mínimo.

## Perfiles y equipos

| Opción | Equilibrado, al pulsarlo | Competitivo, al pulsarlo |
| --- | --- | --- |
| Gráficos / iluminación suave | Detallados / máxima | Rápidos / desactivada |
| Nubes | Rápidas, o desactivadas si ya lo estaban | Desactivadas |
| Partículas | Todas | Reducidas |
| Sombras de entidades | Activadas | Desactivadas |
| Distancia | Conserva la actual | Como máximo 8 chunks; conserva distancias menores |
| Límite FPS, VSync y VBO | Conserva la elección actual | Conserva la elección actual |
| Culling de partículas / agrupación HUD | Activados | Activados |

**Al actualizar o arrancar no se impone un preset de vídeo.** El estado inicial Equilibrado activa las optimizaciones propias y mantiene el vídeo de la instancia. Los valores de la tabla se aplican solamente al pulsar un perfil. Esto permite comenzar en equipos modestos sin aumentarles la distancia existente y conservar límites apropiados para monitores distintos.

Personalizado conserva las opciones actuales y permite ajustarlas individualmente. Cambiar opciones desde Minecraft también se respeta: si difieren del último preset aplicado, el panel identifica el perfil como Personalizado al volver a abrirlo. VBO sigue sujeto a la capacidad del hardware que verifica Minecraft.

**Restaurar anteriores** recupera el vídeo y las dos optimizaciones propios del primer cambio realizado desde el panel. Ese punto de restauración persiste al reiniciar y no se reemplaza al probar varios perfiles. Una vez restaurado, el próximo cambio crea un nuevo punto.

Las opciones vanilla se guardan en `options.txt` usando su rutina original; el perfil, las optimizaciones y el punto de restauración se guardan en `<gameDir>/cubiclient/config.json`. Cambios de distancia, gráficos, iluminación y VBO agrupan sus efectos en una recarga del renderer por acción.

## Optimizaciones implementadas

- **Resolución GUI cacheada:** invalida por ancho, alto, escala y modo Unicode efectivo.
- **HUD agrupado:** las primitivas de atlas de cada widget comparten un bloque de geometría; se conserva el orden de transparencias y se termina el bloque antes de cambiar matrices, dibujar rectángulos vanilla o renderizar objetos de Armor Status. El color pasa por `GlStateManager` para mantener su caché sincronizada.
- **Ruta de HUD vacío:** no dibuja ni cronometra si están desactivados todos los módulos y la marca. FPS/CPS se preparan cuando el módulo correspondiente está activo.
- **Culling conservador de partículas:** antes de emitir un billboard estándar, comprueba su caja interpolada y escalada contra los planos de cámara. Captura el frustum una vez por pasada que tenga partículas elegibles; no consulta GL por partícula. La caja incluye margen de redondeo y conserva efectos que tocan el borde.

El culling actúa en el renderer base `beb`, únicamente durante la pasada normal de `bec`. Los tipos que sobrescriben el método de dibujo y la pasada de efectos especiales quedan fuera. La elegibilidad se resuelve una vez por clase, y los accesos a posición y escala usan handles cacheados en `Game189`. Las partículas siguen creándose y actualizándose normalmente. Un fallo del subsistema desactiva solamente este descarte y conserva el dibujo vanilla.

Estas optimizaciones se pueden desactivar por separado para comparar su coste. El beneficio del culling depende de cuántas partículas elegibles estén fuera de cámara; su propia comprobación también tiene coste.

## Capturar una partida

1. Mantén mundo, posición/recorrido, resolución, paquetes de recursos y ajustes constantes. Deja estabilizar chunks y JVM antes de empezar.
2. En **Rendimiento → Diagnóstico**, pulsa **Iniciar captura** y cierra el menú.
3. Se descartan los primeros **5 segundos** de juego enfocado. Después se registran **60 segundos** de fotogramas de partida, con un límite de **131.072 muestras**. Si el buffer se llena antes, se detiene y lo indica; no sobrescribe muestras.
4. Vuelve al diagnóstico. Puedes detener antes la captura y exportar una muestra parcial. Pulsa **Exportar JSON** para escribir en `<gameDir>/cubiclient/benchmarks/`.

La captura no escribe archivos ni ordena percentiles dentro del hook de fotograma. Usa arrays primitivos fijos que se reservan al iniciar la primera captura. Cambiar de mundo, vídeo o visibilidad del HUD detiene la medición; menús y pérdida de foco no aportan fotogramas. Mantén F1/F3 y la configuración constantes. El buffer y la captura son temporales: exporta antes de salir del juego o iniciar otra captura.

### Interpretación

- **FPS medios:** muestras divididas entre la duración acumulada, no media de FPS instantáneos.
- **p95 / p99:** percentiles de tiempo de fotograma, en milisegundos; menor es mejor. Se usa el rango más cercano por exceso.
- **Etapas:** tiempos transcurridos alrededor de los ticks, del renderer principal, de presentación (`Minecraft.h`, incluyendo `Display.update`) y del limitador `Display.sync`. Se acumulan varios ticks por fotograma. Render incluye mundo e interfaz; las cuatro etapas no cubren todo el trabajo del bucle.
- **Fotograma:** duración de `Minecraft.av()` desde entrada hasta retorno, incluida la espera del limitador. No representa tiempo exclusivo de CPU ni tiempo puro de GPU; no se fuerza una sincronización GPU para medirlo.
- **Heap / GC:** heap al comenzar/terminar y diferencias de los contadores de GC de la JVM. Los contadores abarcan el intervalo real de captura, incluidas pausas en menús o sin foco; no son una atribución de GC por fotograma. Algunos recolectores no publican todos sus contadores.

El JSON contiene muestras brutas, resumen, totales de etapas en orden **tick, render, presentación, limitador**, y contexto: GPU/driver, Java, sistema, procesadores disponibles, heap máximo, resolución, vídeo y configuración Cubi. La herramienta no identifica automáticamente CPU física, escena o recorrido: anótalos al comparar equipos.

```bash
python3 tools/compare_performance.py antes.json despues.json
```

El comparador recalcula los resultados desde las muestras e indica diferencias entre los contextos registrados. Repite al menos tres veces por escenario: terreno estable, desplazamiento con carga de chunks, partículas y zonas con muchos objetos. Compara primero con vídeo idéntico y alterna activación de las optimizaciones. Después evalúa los perfiles por separado: reducir calidad también reduce trabajo, pero cambia las condiciones de comparación.

La captura interna pertenece a Cubi; una comparación contra vanilla o Lunar necesita la misma herramienta externa de frametimes para todos los clientes. El smoke test es una comprobación funcional, no una medición A/B. **No hay todavía una ganancia porcentual validada ni una comparación de paridad con Lunar.** La planificación de chunks, la oclusión de entidades y el culling individual de entidades de bloque son trabajo posterior guiado por perfiles.
