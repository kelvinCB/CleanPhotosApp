package com.kelvincalcano.cleanphotos.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.content.ContentResolver
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelvincalcano.cleanphotos.domain.PhotoAlbum
import com.kelvincalcano.cleanphotos.domain.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AlbumSelectionScreen(
    albums: List<PhotoAlbum>,
    onAlbumSelected: (PhotoAlbum) -> Unit,
) {
    val resolver = LocalContext.current.contentResolver
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("Elige un álbum", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Selecciona de dónde quieres revisar fotos. Puedes empezar por Capturas de pantalla, WhatsApp o Cámara.",
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (albums.isEmpty()) {
            Text("No se encontraron álbumes con fotos.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(albums, key = PhotoAlbum::id) { album ->
                    AlbumRow(
                        album = album,
                        resolver = resolver,
                        onClick = { onAlbumSelected(album) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumRow(
    album: PhotoAlbum,
    resolver: ContentResolver,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Álbum ${album.name}" },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AlbumCover(album.coverUri, resolver)
            Column {
                Text(album.name, style = MaterialTheme.typography.titleMedium)
                Text("${album.photoCount} fotos")
                Text(
                    formatBytes(album.totalBytes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun AlbumCover(coverUri: String?, resolver: ContentResolver) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = coverUri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (coverUri == null) return@runCatching null
                val source = ImageDecoder.createSource(resolver, Uri.parse(coverUri))
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSize(160, 160)
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }.getOrNull()
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text("Foto", color = Color.Gray)
        }
    }
}
