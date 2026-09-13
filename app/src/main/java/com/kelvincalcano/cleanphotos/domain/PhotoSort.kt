package com.kelvincalcano.cleanphotos.domain

enum class PhotoSortOrder(val label: String) {
    DATE_ASC("Fecha: más antigua primero"),
    DATE_DESC("Fecha: más reciente primero"),
    SIZE_DESC("Tamaño: mayor a menor"),
    SIZE_ASC("Tamaño: menor a mayor"),
}

fun sortPhotos(photos: List<Photo>, order: PhotoSortOrder): List<Photo> = when (order) {
    PhotoSortOrder.DATE_ASC -> photos.sortedWith(
        compareBy<Photo> { it.dateAddedSeconds }.thenBy { it.id },
    )
    PhotoSortOrder.DATE_DESC -> photos.sortedWith(
        compareByDescending<Photo> { it.dateAddedSeconds }.thenBy { it.id },
    )
    PhotoSortOrder.SIZE_DESC -> photos.sortedWith(
        compareByDescending<Photo> { it.sizeBytes }.thenBy { it.id },
    )
    PhotoSortOrder.SIZE_ASC -> photos.sortedWith(
        compareBy<Photo> { it.sizeBytes }.thenBy { it.id },
    )
}
