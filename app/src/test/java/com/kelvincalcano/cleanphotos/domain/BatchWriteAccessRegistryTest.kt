package com.kelvincalcano.cleanphotos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchWriteAccessRegistryTest {
    @Test
    fun `granting a batch removes its photos from future access requests`() {
        val registry = BatchWriteAccessRegistry()
        val batch = listOf(photo(1), photo(2))

        assertEquals(batch.map(Photo::uri), registry.missingUris(batch))

        registry.grant(batch.map(Photo::uri))

        assertTrue(registry.missingUris(batch).isEmpty())
        assertTrue(registry.hasAccess(photo(1)))
    }

    @Test
    fun `a new activity session starts without write access`() {
        val firstSession = BatchWriteAccessRegistry()
        firstSession.grant(listOf(photo(1).uri))

        val restartedSession = BatchWriteAccessRegistry()

        assertEquals(listOf(photo(1).uri), restartedSession.missingUris(listOf(photo(1))))
    }

    private fun photo(id: Long) = Photo(
        id = id,
        uri = "content://media/external/images/media/$id",
        displayName = "photo-$id.jpg",
        sizeBytes = 1_024L,
    )
}
