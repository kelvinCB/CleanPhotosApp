package com.kelvincalcano.cleanphotos.domain

/**
 * Tracks photos the user chose to keep and persists the URI set through the
 * supplied storage callback.
 */
class KeptPhotoStore(
    initiallyKeptUris: Set<String> = emptySet(),
    private val persist: (Set<String>) -> Unit,
) {
    private val keptUris = initiallyKeptUris.toMutableSet()

    fun markKept(photo: Photo): Boolean {
        if (!keptUris.add(photo.uri)) return false
        persist(keptUris.toSet())
        return true
    }

    fun unmarkKept(photo: Photo): Boolean {
        if (!keptUris.remove(photo.uri)) return false
        persist(keptUris.toSet())
        return true
    }

    fun unmarkKept(photos: Collection<Photo>): Int {
        val removedUris = photos.map(Photo::uri).filter(keptUris::contains).toSet()
        if (removedUris.isEmpty()) return 0
        keptUris.removeAll(removedUris)
        persist(keptUris.toSet())
        return removedUris.size
    }

    fun isKept(photo: Photo): Boolean = photo.uri in keptUris

    fun filterUnreviewed(photos: List<Photo>): List<Photo> = photos.filterNot(::isKept)
}
