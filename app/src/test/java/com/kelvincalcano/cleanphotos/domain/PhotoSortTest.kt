package com.kelvincalcano.cleanphotos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoSortTest {
    private val oldestSmall = photo(id = 1L, dateAdded = 10L, sizeBytes = 100L)
    private val newestLarge = photo(id = 2L, dateAdded = 30L, sizeBytes = 300L)
    private val middle = photo(id = 3L, dateAdded = 20L, sizeBytes = 200L)

    @Test
    fun `date sorting supports oldest and newest first`() {
        val photos = listOf(middle, newestLarge, oldestSmall)

        assertEquals(
            listOf(oldestSmall, middle, newestLarge),
            sortPhotos(photos, PhotoSortOrder.DATE_ASC),
        )
        assertEquals(
            listOf(newestLarge, middle, oldestSmall),
            sortPhotos(photos, PhotoSortOrder.DATE_DESC),
        )
    }

    @Test
    fun `size sorting supports largest and smallest first`() {
        val photos = listOf(middle, newestLarge, oldestSmall)

        assertEquals(
            listOf(newestLarge, middle, oldestSmall),
            sortPhotos(photos, PhotoSortOrder.SIZE_DESC),
        )
        assertEquals(
            listOf(oldestSmall, middle, newestLarge),
            sortPhotos(photos, PhotoSortOrder.SIZE_ASC),
        )
    }

    private fun photo(id: Long, dateAdded: Long, sizeBytes: Long) = Photo(
        id = id,
        uri = "content://media/external/images/media/$id",
        displayName = "photo-$id.jpg",
        sizeBytes = sizeBytes,
        dateAddedSeconds = dateAdded,
    )
}
