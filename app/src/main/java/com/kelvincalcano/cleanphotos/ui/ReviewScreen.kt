package com.kelvincalcano.cleanphotos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelvincalcano.cleanphotos.domain.ReviewSessionSnapshot
import com.kelvincalcano.cleanphotos.domain.formatBytes

@Composable
fun ReviewScreen(
    state: ReviewSessionSnapshot,
    onKeep: () -> Unit,
    onTrash: () -> Unit,
    onLoadMore: () -> Unit,
    onUndo: () -> Unit = {},
    feedback: String? = null,
    actionsEnabled: Boolean = true,
    showBatchWriteExplanation: Boolean = false,
    onBatchWriteExplanationAccepted: () -> Unit = {},
    onBatchWriteExplanationDismissed: () -> Unit = {},
    albumName: String? = null,
    albumPhotoCount: Int? = null,
    onChangeAlbum: () -> Unit = {},
    onReactivatePhotos: () -> Unit = {},
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
                if (albumName != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Álbum: $albumName",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onChangeAlbum) { Text("Cambiar") }
                    }
                }
                Text(
                    text = "Foto ${state.reviewedCount + 1} de ${state.batchTotal}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                PhotoStack(
                    state = state,
                    resolver = resolver,
                    onKeep = onKeep,
                    onTrash = onTrash,
                    actionsEnabled = actionsEnabled,
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
                if (state.canUndo) {
                    TextButton(
                        onClick = onUndo,
                        enabled = actionsEnabled && !state.pendingTrash,
                    ) { Text("Recuperar foto") }
                }
            }

            !state.hasPhotos && albumPhotoCount != null && albumPhotoCount > 0 ->
                CompletedAlbumState(
                    albumName = albumName.orEmpty(),
                    photoCount = albumPhotoCount,
                    onChangeAlbum = onChangeAlbum,
                    onReactivatePhotos = onReactivatePhotos,
                )

            !state.hasPhotos -> EmptyState(onChangeAlbum = onChangeAlbum)

            else -> {
                SummaryState(
                    state = state,
                    onLoadMore = onLoadMore,
                    onChangeAlbum = onChangeAlbum,
                    onUndo = onUndo,
                )
            }
        }

        if (!feedback.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(feedback, color = MaterialTheme.colorScheme.primary)
        }
    }

    if (showBatchWriteExplanation) {
        AlertDialog(
            onDismissRequest = onBatchWriteExplanationDismissed,
            title = { Text("Permiso para limpiar fotos") },
            text = {
                Text(
                    "Clean Photos necesita autorización para enviar a la papelera las fotos que selecciones.",
                )
            },
            confirmButton = {
                TextButton(onClick = onBatchWriteExplanationAccepted) {
                    Text("Entendido")
                }
            },
        )
    }
}

@Composable
private fun SummaryState(
    state: ReviewSessionSnapshot,
    onLoadMore: () -> Unit,
    onChangeAlbum: () -> Unit,
    onUndo: () -> Unit,
) {
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
            Button(onClick = onLoadMore) { Text("Cargar ${state.batchSize} más") }
        } else {
            Text("No quedan más fotos para revisar.")
        }
        if (state.canUndo) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onUndo) { Text("Recuperar foto") }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onChangeAlbum) { Text("Elegir otro álbum") }
    }
}

@Composable
private fun PhotoStack(
    state: ReviewSessionSnapshot,
    resolver: android.content.ContentResolver,
    onKeep: () -> Unit,
    onTrash: () -> Unit,
    actionsEnabled: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (state.previousPhotos.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Anteriores",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.previousPhotos.forEach { previousPhoto ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .semantics {
                                    contentDescription = "Foto anterior ${previousPhoto.displayName}"
                                },
                        ) {
                            PhotoThumbnail(previousPhoto, resolver, thumbnailHeight = 72.dp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        state.current?.let { photo ->
            PhotoCard(
                photo = photo,
                resolver = resolver,
                onSwipeLeft = onKeep,
                onSwipeRight = onTrash,
                isPending = state.pendingTrash || !actionsEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EmptyState(onChangeAlbum: () -> Unit) {
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
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onChangeAlbum) { Text("Elegir otro álbum") }
    }
}

@Composable
private fun CompletedAlbumState(
    albumName: String,
    photoCount: Int,
    onChangeAlbum: () -> Unit,
    onReactivatePhotos: () -> Unit,
) {
    val isSingular = photoCount == 1
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Álbum completado", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (isSingular) {
                "La única foto de $albumName ya está marcada como conservada."
            } else {
                "Las $photoCount fotos de $albumName ya están marcadas como conservadas."
            },
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onReactivatePhotos) {
            Text("Volver a activar fotos para clasificarlas")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onChangeAlbum) { Text("Elegir otro álbum") }
    }
}

@Composable
fun LoadingState(message: String = "Cargando tus fotos…") {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message)
    }
}
