# Clean Photos

Aplicación Android local para revisar imágenes en tarjetas, conservarlas con swipe a la izquierda y moverlas a la papelera del sistema con swipe a la derecha.

## Comportamiento

- Consulta todas las imágenes indexadas por `MediaStore.Images` en el volumen externo.
- Incluye cámara, capturas de pantalla, WhatsApp y formatos de imagen que Android indexe.
- Trabaja en lotes de 200 y no copia las fotos fuera del dispositivo.
- La eliminación usa la confirmación oficial de Android. Una foto solo desaparece del flujo después de confirmar.
- El resumen muestra el tamaño enviado a papelera. Android puede tardar en liberar físicamente ese espacio hasta vaciar la papelera.

## Compilar

Con Java 17 y el SDK Android configurados:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-daemon --console=plain
```

Para ejecutar pruebas Compose en un dispositivo autorizado:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon --console=plain
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

