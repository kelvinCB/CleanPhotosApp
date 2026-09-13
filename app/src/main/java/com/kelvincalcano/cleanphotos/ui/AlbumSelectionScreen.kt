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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.kelvincalcano.cleanphotos.domain.AlbumSortOrder
import com.kelvincalcano.cleanphotos.domain.PhotoAlbum
import com.kelvincalcano.cleanphotos.domain.PhotoSortOrder
import com.kelvincalcano.cleanphotos.domain.formatBytes
import com.kelvincalcano.cleanphotos.domain.sortAlbums
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AlbumSelectionScreen(
    albums: List<PhotoAlbum>,
    onAlbumSelected: (PhotoAlbum) -> Unit,
    photoSortOrder: PhotoSortOrder = PhotoSortOrder.DATE_ASC,
    onPhotoSortOrderChanged: (PhotoSortOrder) -> Unit = {},
) {
    val resolver = LocalContext.current.contentResolver
    var sortOrderName by rememberSaveable { mutableStateOf(AlbumSortOrder.NAME_ASC.name) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var isPhotoSortMenuExpanded by remember { mutableStateOf(false) }
    val sortOrder = AlbumSortOrder.valueOf(sortOrderName)
    val sortedAlbums = remember(albums, sortOrder) { sortAlbums(albums, sortOrder) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("Elige un álbum", style = MaterialTheme.typography.headlineSmall)

        if (albums.isEmpty()) {
            Text("No se encontraron álbumes con fotos.")
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Ordenar álbumes", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Box {
                    OutlinedButton(
                        onClick = { isSortMenuExpanded = true },
                        modifier = Modifier.semantics {
                            contentDescription = "Orden actual: ${sortOrder.label}"
                        },
                    ) {
                        Text(sortOrder.label)
                    }
                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false },
                    ) {
                        AlbumSortOrder.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    sortOrderName = option.name
                                    isSortMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Ordenar fotos", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Box {
                    OutlinedButton(
                        onClick = { isPhotoSortMenuExpanded = true },
                        modifier = Modifier.semantics {
                            contentDescription = "Orden actual de fotos: ${photoSortOrder.label}"
                        },
                    ) {
                        Text(photoSortOrder.label)
                    }
                    DropdownMenu(
                        expanded = isPhotoSortMenuExpanded,
                        onDismissRequest = { isPhotoSortMenuExpanded = false },
                    ) {
                        PhotoSortOrder.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    onPhotoSortOrderChanged(option)
                                    isPhotoSortMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(sortedAlbums, key = PhotoAlbum::id) { album ->
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AlbumCover(album.coverUri, resolver)
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
