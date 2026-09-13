package com.kelvincalcano.cleanphotos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoRepositoryContractTest {
    @Test
    fun `repository filters out trashed media and sorts oldest first`() {
        assertEquals("is_trashed = 0", PhotoRepository.selection)
        assertEquals("date_added ASC, _id ASC", PhotoRepository.sortOrder)
    }

    @Test
    fun `repository maps image metadata to a photo`() {
        val photo = PhotoRepository.mapRow(
            id = 42L,
            displayName = "Screenshot.png",
            sizeBytes = 8_192L,
            uri = "content://media/external/images/media/42",
        )

        assertEquals(42L, photo?.id)
        assertEquals("Screenshot.png", photo?.displayName)
        assertEquals(8_192L, photo?.sizeBytes)
        assertEquals("content://media/external/images/media/42", photo?.uri)
    }

    @Test
    fun `repository ignores rows without an id or uri`() {
        assertNull(PhotoRepository.mapRow(null, "missing-id.jpg", 1L, "content://media/1"))
        assertNull(PhotoRepository.mapRow(1L, "missing-uri.jpg", 1L, null))
    }
}

