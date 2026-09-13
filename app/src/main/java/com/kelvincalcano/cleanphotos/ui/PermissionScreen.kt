package com.kelvincalcano.cleanphotos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    partialAccess: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Clean Photos", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = if (partialAccess) {
                "La app solo tiene acceso a algunas fotos. Para revisar cámara, capturas y WhatsApp, permite el acceso a todas las fotos."
            } else {
                "Necesitamos acceso a tus fotos para mostrarte toda la galería y ayudarte a limpiarla. Las fotos no salen del dispositivo."
            },
            modifier = Modifier.padding(vertical = 18.dp),
        )
        Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
            Text(if (partialAccess) "Elegir todas las fotos" else "Dar acceso a mis fotos")
        }
        if (partialAccess) {
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir Ajustes")
            }
        }
    }
}

