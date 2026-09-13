package com.kelvincalcano.cleanphotos.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import com.kelvincalcano.cleanphotos.domain.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoRepository(private val resolver: ContentResolver) {
    suspend fun loadPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        resolver.query(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            PROJECTION,
            selection,
            null,
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

    internal companion object {
        const val selection = "${MediaStore.MediaColumns.IS_TRASHED} = 0"
        const val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC, ${MediaStore.Images.Media._ID} ASC"

        private val PROJECTION = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
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
}

