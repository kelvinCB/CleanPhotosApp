# Clean Photos — diseño aprobado

## Objetivo

Crear una aplicación Android ligera para revisar todas las imágenes indexadas en el dispositivo en lotes de 200. Un swipe a la izquierda conserva la imagen; un swipe a la derecha solicita moverla a la papelera real del sistema. Al terminar cada lote, la app muestra el resumen y permite cargar 200 imágenes adicionales.

## Alcance funcional

- Consultar `MediaStore.Images` para incluir fotos de cámara, capturas de pantalla, WhatsApp y todos los formatos de imagen que Android tenga indexados.
- Excluir elementos que ya estén en la papelera y no consultar ni modificar vídeos.
- Mostrar una tarjeta grande con la imagen actual y feedback visual breve: “Conservar” para izquierda, “Enviada a papelera” para derecha.
- Procesar como máximo 200 tarjetas por lote; no cargar los bytes de todas las fotos en memoria.
- Después de cada lote, informar revisadas, conservadas, enviadas a papelera y tamaño acumulado enviado a papelera. Ofrecer “Cargar 200 más” o finalizar si no quedan imágenes.
- Consultar el tamaño original (`MediaStore.MediaColumns.SIZE`) y formatearlo en B/KB/MB/GB.
- Explicar que mover a papelera no garantiza liberar espacio físico inmediatamente: la liberación efectiva depende de cuándo Android vacíe la papelera.

## Privacidad y permisos

- Usar solo `READ_MEDIA_IMAGES` en Android 13+ y `READ_EXTERNAL_STORAGE` en Android 12 o anterior.
- En Android 14+, si el sistema concede acceso parcial, mostrar una pantalla de acceso incompleto y pedir al usuario que conceda acceso a todas las fotos desde el diálogo o Ajustes.
- No subir, duplicar ni mover archivos a otra carpeta.
- Para borrar elementos que no pertenecen a la app, usar `MediaStore.createTrashRequest(..., true)` y un `IntentSender` oficial. La tarjeta solo se retira definitivamente del flujo después de recibir `RESULT_OK`.

## Arquitectura

- Kotlin, Android SDK y Jetpack Compose.
- `PhotoRepository`: consulta metadatos y crea solicitudes de papelera.
- `ReviewSession`: máquina de estados pura para lotes, decisiones, resultados confirmados y resumen.
- `MainActivity`: permisos, lanzador de confirmación del sistema y composición de la UI.
- `ReviewScreen`: gesto, tarjeta, animación y resumen; no contiene consultas directas a `ContentResolver`.
- Coil Compose para decodificación eficiente de miniaturas desde `content://`; solo se mantienen pocas imágenes cercanas al frente.

## Criterios de aceptación

1. En un teléfono físico Android se puede conceder acceso completo a las fotos y aparece la primera foto indexada.
2. Swipe izquierdo avanza sin borrar y aumenta el contador de conservadas.
3. Swipe derecho abre la confirmación de Android; al aceptar, la imagen desaparece, aumenta el contador de papelera y suma sus bytes.
4. Si se cancela la confirmación, la imagen sigue disponible y no se suma espacio.
5. El lote se detiene en 200 decisiones confirmadas y muestra su resumen.
6. “Cargar 200 más” continúa sin reiniciar la sesión ni repetir elementos.
7. Cuando no quedan imágenes, se muestra el estado final y el espacio enviado a papelera.
8. Las pruebas unitarias cubren la máquina de estados, el cálculo de lotes, cancelación de papelera y formato de espacio.

