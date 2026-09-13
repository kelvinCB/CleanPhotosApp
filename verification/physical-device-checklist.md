# Verificación física — 2026-09-12

Dispositivo: Samsung SM-S938B, serial `R5CY5249KYX`, Android 16.

- [x] APK instalado con éxito como `com.kelvincalcano.cleanphotos`.
- [x] Permiso del sistema concedido como “Permitir todo”.
- [x] Primera foto real detectada desde la biblioteca del dispositivo.
- [x] Swipe izquierdo: pasó de “Foto 1 de 200” a “Foto 2 de 200” y mostró “Conservada”.
- [x] Swipe derecho: Android mostró “¿Deseas permitir que Clean Photos mueva esta foto a la papelera?” con la miniatura real.
- [x] Confirmación “Permitir”: la app avanzó a “Foto 3 de 200” y mostró “Enviada a papelera”.
- [x] Cancelación “Rechazar”: la tercera foto permaneció en pantalla y mostró “No se movió la foto a la papelera”.
- [x] `logcat -b crash`: sin salida durante el flujo probado.
- [x] Captura de pantalla de la prueba conservada localmente y excluida del repositorio público por contener una foto personal.

El flujo de 200 fotos no se completó durante la prueba porque hacerlo habría requerido tomar 198 decisiones adicionales sobre fotos personales. La lógica de corte de 200 y el resumen de bytes están cubiertos por pruebas unitarias.
