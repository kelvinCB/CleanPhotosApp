package com.kelvincalcano.cleanphotos.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.ContentUris
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import com.kelvincalcano.cleanphotos.domain.Photo
import com.kelvincalcano.cleanphotos.domain.PhotoAlbum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoRepository(private val resolver: ContentResolver) {
    private val mediaCollection by lazy {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    }

    suspend fun loadAlbums(): List<PhotoAlbum> = withContext(Dispatchers.IO) {
        val albums = linkedMapOf<String, AlbumAccumulator>()
        resolver.query(
            mediaCollection,
            ALBUM_PROJECTION,
            selection,
            null,
            sortOrder,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdIndex).orEmpty()
                val bucketName = cursor.getString(bucketNameIndex).orEmpty()
                val coverUri = ContentUris.withAppendedId(
                    mediaCollection,
                    cursor.getLong(idIndex),
                ).toString()
                val accumulator = albums.getOrPut(bucketId) {
                    AlbumAccumulator(bucketId, bucketName, coverUri)
                }
                accumulator.photoCount++
                accumulator.totalBytes += cursor.getLong(sizeIndex).coerceAtLeast(0L)
            }
        }

        if (albums.isEmpty()) return@withContext emptyList()
        val allPhotos = PhotoAlbum.allPhotos(
            photoCount = albums.values.sumOf(AlbumAccumulator::photoCount),
            totalBytes = albums.values.sumOf(AlbumAccumulator::totalBytes),
            coverUri = albums.values.first().coverUri,
        )
        buildList {
            add(allPhotos)
            addAll(
                albums.values
                    .filter { it.photoCount > 0 }
                    .map { accumulator ->
                        mapAlbumRow(
                            bucketId = accumulator.bucketId,
                            bucketName = accumulator.bucketName,
                            coverUri = accumulator.coverUri,
                            photoCount = accumulator.photoCount,
                            totalBytes = accumulator.totalBytes,
                        )
                    }
                    .sortedBy { it.name.lowercase() },
            )
        }
    }

    suspend fun loadPhotos(album: PhotoAlbum? = null): List<Photo> = withContext(Dispatchers.IO) {
        val albumSelection = if (album == null || album.isAllPhotos) {
            selection
        } else {
            "$selection AND ${MediaStore.Images.Media.BUCKET_ID} = ?"
        }
        val albumSelectionArgs = if (album == null || album.isAllPhotos) {
            null
        } else {
            arrayOf(album.id)
        }
        resolver.query(
            mediaCollection,
            PROJECTION,
            albumSelection,
            albumSelectionArgs,
            sortOrder,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                        id,
                    ).toString()
                    add(
                        mapRow(
                            id = id,
                            displayName = cursor.getString(nameIndex),
                            sizeBytes = cursor.getLong(sizeIndex),
                            uri = uri,
                        ) ?: continue,
                    )
                }
            }
        } ?: emptyList()
    }

    fun createTrashRequest(photo: Photo): IntentSender =
        MediaStore.createTrashRequest(resolver, listOf(Uri.parse(photo.uri)), true).intentSender

    fun createWriteRequest(photos: Collection<Photo>): IntentSender =
        MediaStore.createWriteRequest(resolver, photos.map { Uri.parse(it.uri) }).intentSender

    fun moveToTrash(photo: Photo): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_TRASHED, 1)
        }
        return resolver.update(Uri.parse(photo.uri), values, null, null) == 1
    }

    internal companion object {
        const val selection = "${MediaStore.MediaColumns.IS_TRASHED} = 0"
        const val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC, ${MediaStore.Images.Media._ID} ASC"
        private val PROJECTION = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
        )

        private val ALBUM_PROJECTION = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
        )

        fun mapAlbumRow(
            bucketId: String,
            bucketName: String?,
            coverUri: String?,
            photoCount: Int,
            totalBytes: Long,
        ) = PhotoAlbum(
            id = bucketId,
            name = bucketName.orEmpty().ifBlank { "Sin álbum" },
            coverUri = coverUri,
            photoCount = photoCount,
            totalBytes = totalBytes.coerceAtLeast(0L),
        )

        fun mapRow(id: Long?, displayName: String?, sizeBytes: Long?, uri: String?): Photo? {
            if (id == null || uri.isNullOrBlank()) return null
            return Photo(
                id = id,
                uri = uri,
                displayName = displayName.orEmpty(),
                sizeBytes = sizeBytes ?: 0L,
            )
        }
    }

    private data class AlbumAccumulator(
        val bucketId: String,
        val bucketName: String,
        val coverUri: String,
        var photoCount: Int = 0,
        var totalBytes: Long = 0L,
    )
}
