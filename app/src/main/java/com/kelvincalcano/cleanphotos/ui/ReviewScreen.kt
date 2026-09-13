package com.kelvincalcano.cleanphotos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kelvincalcano.cleanphotos.domain.ReviewSessionSnapshot
import com.kelvincalcano.cleanphotos.domain.formatBytes

@Composable
fun ReviewScreen(
    state: ReviewSessionSnapshot,
    onKeep: () -> Unit,
    onTrash: () -> Unit,
    onLoadMore: () -> Unit,
    feedback: String? = null,
    actionsEnabled: Boolean = true,
) {
    val resolver = LocalContext.current.contentResolver
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Clean Photos", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))

        when {
            state.current != null -> {
                Text(
                    text = "Foto ${state.reviewedCount + 1} de ${state.batchTotal}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                PhotoCard(
                    photo = state.current,
                    resolver = resolver,
                    onSwipeLeft = onKeep,
                    onSwipeRight = onTrash,
                    isPending = state.pendingTrash || !actionsEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onKeep,
                        enabled = actionsEnabled && !state.pendingTrash,
                        modifier = Modifier.weight(1f),
                    ) { Text("Conservar") }
                    Button(
                        onClick = onTrash,
                        enabled = actionsEnabled && !state.pendingTrash,
                        modifier = Modifier.weight(1f),
                    ) { Text("Papelera") }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Desliza izquierda para conservar · derecha para papelera",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            !state.hasPhotos -> EmptyState()

            else -> {
                SummaryState(
                    state = state,
                    onLoadMore = onLoadMore,
                )
            }
        }

        if (!feedback.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(feedback, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SummaryState(state: ReviewSessionSnapshot, onLoadMore: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Lote terminado", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(18.dp))
        Text("Revisadas: ${state.reviewedCount}")
        Text("Conservadas: ${state.keptCount}")
        Text("Enviadas a papelera: ${state.trashedCount}")
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text(
            "Espacio enviado a papelera: ${formatBytes(state.trashedBytes)}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Android puede liberarlo físicamente cuando vacíe la papelera.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        if (state.hasMorePhotos) {
            Button(onClick = onLoadMore) { Text("Cargar 200 más") }
        } else {
            Text("No quedan más fotos para revisar.")
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("No hay fotos disponibles", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("No se encontraron imágenes en la galería del dispositivo.")
    }
}

@Composable
fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Cargando tus fotos…")
    }
}
