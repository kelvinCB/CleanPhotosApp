# Clean Photos — diseño aprobado

## Objetivo

Crear una aplicación Android ligera para revisar todas las imágenes indexadas en el dispositivo en lotes de 300. Un swipe a la izquierda conserva la imagen; un swipe a la derecha solicita moverla a la papelera real del sistema. Al terminar cada lote, la app muestra el resumen y permite cargar 300 imágenes adicionales o elegir otro álbum.

## Alcance funcional

- Consultar `MediaStore.Images` para incluir fotos de cámara, capturas de pantalla, WhatsApp y todos los formatos de imagen que Android tenga indexados.
- Excluir elementos que ya estén en la papelera y no consultar ni modificar vídeos.
- Mostrar una tarjeta grande con la imagen actual y feedback visual breve: “Conservar” para izquierda, “Enviada a papelera” para derecha.
- Procesar como máximo 300 tarjetas por lote; no cargar los bytes de todas las fotos en memoria.
- Después de cada lote, informar revisadas, conservadas, enviadas a papelera y tamaño acumulado enviado a papelera. Ofrecer “Cargar 300 más” o finalizar si no quedan imágenes.
- Cuando el lote o el álbum termina, ofrecer “Elegir otro álbum” para volver a la selección sin perder las decisiones ya tomadas.
- Si el álbum contiene imágenes, pero todas fueron conservadas o enviadas a papelera en sesiones anteriores, mostrar “Álbum completado” con el nombre y la cantidad, en lugar del estado genérico sin fotos.
- Tras conservar una foto, mostrar hasta tres miniaturas de fotos conservadas anteriores en una franja separada, encima de la tarjeta actual y sin cubrirla.
- “Recuperar foto” solo debe deshacer la última decisión de conservar y quitar esa foto del registro persistente de conservadas. Las fotos enviadas a papelera no ofrecen deshacer desde la pantalla de revisión.
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
5. El lote se detiene en 300 decisiones confirmadas y muestra su resumen.
6. “Cargar 300 más” continúa sin reiniciar la sesión ni repetir elementos.
7. Cuando no quedan imágenes, se muestra el estado final, el espacio enviado a papelera y “Elegir otro álbum”.
8. “Elegir otro álbum” vuelve a la selección de álbumes y permite iniciar la revisión de otro álbum.
9. Si el filtro de fotos pendientes queda vacío y el álbum original tenía imágenes, se muestra el mensaje de álbum completado y “Elegir otro álbum”.
9. Las miniaturas anteriores aparecen fuera de la tarjeta actual y nunca se superponen con ella.
10. “Recuperar foto” aparece para una conservación, restaura esa foto como actual y no aparece después de una decisión de papelera.
11. Las pruebas unitarias cubren la máquina de estados, el cálculo de lotes, cancelación de papelera, recuperación de conservadas y formato de espacio; las pruebas de UI cubren la salida de un álbum corto, la separación visual y la ausencia de recuperación tras papelera.
