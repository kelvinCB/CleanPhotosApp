package com.kelvincalcano.cleanphotos.domain

data class PhotoAlbum(
    val id: String,
    val name: String,
    val coverUri: String?,
    val photoCount: Int,
    val totalBytes: Long,
    val isAllPhotos: Boolean = false,
    val latestPhotoDateAdded: Long = 0L,
) {
    companion object {
        private const val ALL_PHOTOS_ID = "__all_photos__"

        fun allPhotos(
            photoCount: Int,
            totalBytes: Long,
            coverUri: String? = null,
            latestPhotoDateAdded: Long = 0L,
        ) = PhotoAlbum(
            id = ALL_PHOTOS_ID,
            name = "Todas las fotos",
            coverUri = coverUri,
            photoCount = photoCount,
            totalBytes = totalBytes,
            isAllPhotos = true,
            latestPhotoDateAdded = latestPhotoDateAdded,
        )
    }
}
