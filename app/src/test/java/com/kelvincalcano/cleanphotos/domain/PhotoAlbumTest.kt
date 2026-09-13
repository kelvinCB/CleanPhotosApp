package com.kelvincalcano.cleanphotos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoAlbumTest {
    @Test
    fun `all photos option is identifiable`() {
        val album = PhotoAlbum.allPhotos(photoCount = 12, totalBytes = 24_576L)

        assertTrue(album.isAllPhotos)
        assertEquals("Todas las fotos", album.name)
        assertEquals(12, album.photoCount)
        assertEquals(24_576L, album.totalBytes)
    }
}
