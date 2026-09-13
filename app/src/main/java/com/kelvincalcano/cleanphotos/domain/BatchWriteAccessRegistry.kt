package com.kelvincalcano.cleanphotos.domain

/** Tracks the media URIs covered by the current activity-scoped write grant. */
class BatchWriteAccessRegistry(
    initiallyGrantedUris: Set<String> = emptySet(),
) {
    private val grantedUris = initiallyGrantedUris.toMutableSet()

    fun missingUris(photos: List<Photo>): List<String> = photos
        .map(Photo::uri)
        .filterNot(grantedUris::contains)
        .distinct()

    fun grant(uris: Collection<String>) {
        grantedUris += uris
    }

    fun hasAccess(photo: Photo): Boolean = photo.uri in grantedUris
}
