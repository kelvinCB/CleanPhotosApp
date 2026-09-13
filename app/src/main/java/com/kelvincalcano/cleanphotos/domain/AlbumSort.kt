package com.kelvincalcano.cleanphotos.domain

enum class AlbumSortOrder(val label: String) {
    SIZE_DESC("Tamaño: mayor a menor"),
    SIZE_ASC("Tamaño: menor a mayor"),
    NAME_ASC("Nombre: A a Z"),
    NAME_DESC("Nombre: Z a A"),
    RECENT_DESC("Foto más reciente primero"),
    RECENT_ASC("Foto más antigua primero"),
}

fun sortAlbums(albums: List<PhotoAlbum>, order: AlbumSortOrder): List<PhotoAlbum> {
    val allPhotos = albums.filter { it.isAllPhotos }
    val otherAlbums = albums.filterNot { it.isAllPhotos }
    val sortedAlbums = when (order) {
        AlbumSortOrder.SIZE_DESC -> otherAlbums.sortedWith(
            compareByDescending<PhotoAlbum> { it.totalBytes }.thenBy { it.name.lowercase() },
        )
        AlbumSortOrder.SIZE_ASC -> otherAlbums.sortedWith(
            compareBy<PhotoAlbum> { it.totalBytes }.thenBy { it.name.lowercase() },
        )
        AlbumSortOrder.NAME_ASC -> otherAlbums.sortedWith(
            compareBy<PhotoAlbum> { it.name.lowercase() }.thenBy { it.id },
        )
        AlbumSortOrder.NAME_DESC -> otherAlbums.sortedWith(
            compareByDescending<PhotoAlbum> { it.name.lowercase() }.thenBy { it.id },
        )
        AlbumSortOrder.RECENT_DESC -> otherAlbums.sortedWith(
            compareByDescending<PhotoAlbum> { it.latestPhotoDateAdded }.thenBy { it.name.lowercase() },
        )
        AlbumSortOrder.RECENT_ASC -> otherAlbums.sortedWith(
            compareBy<PhotoAlbum> { it.latestPhotoDateAdded }.thenBy { it.name.lowercase() },
        )
    }
    return allPhotos + sortedAlbums
}
