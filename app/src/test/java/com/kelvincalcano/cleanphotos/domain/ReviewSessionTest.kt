package com.kelvincalcano.cleanphotos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSessionTest {
    @Test
    fun `loaded batch exposes first photo and left decision keeps it`() {
        val session = ReviewSession(batchSize = 2)
        session.load(listOf(photo(1), photo(2), photo(3)))

        assertEquals(1L, session.current?.id)
        assertTrue(session.keepCurrent())
        assertEquals(2L, session.current?.id)
        assertEquals(1, session.keptCount)
        assertEquals(1, session.reviewedCount)
    }

    @Test
    fun `confirmed trash advances and sums the selected photo size`() {
        val session = ReviewSession(batchSize = 2)
        session.load(listOf(photo(1, sizeBytes = 4_000), photo(2)))

        val pending = session.requestTrashCurrent()

        assertNotNull(pending)
        assertTrue(session.confirmTrash(1L))
        assertEquals(2L, session.current?.id)
        assertEquals(1, session.trashedCount)
        assertEquals(4_000L, session.trashedBytes)
    }

    @Test
    fun `cancelled trash leaves the current photo and counters unchanged`() {
        val session = ReviewSession(batchSize = 2)
        session.load(listOf(photo(1)))

        session.requestTrashCurrent()

        assertTrue(session.cancelTrash(1L))
        assertEquals(1L, session.current?.id)
        assertEquals(0, session.reviewedCount)
        assertEquals(0, session.trashedCount)
        assertEquals(0L, session.trashedBytes)
    }

    @Test
    fun `batch stops at 200 and loads remaining photos only on request`() {
        val photos = (1L..201L).map(::photo)
        val session = ReviewSession(batchSize = 200)
        session.load(photos)

        repeat(200) { assertTrue(session.keepCurrent()) }

        assertTrue(session.isBatchComplete)
        assertTrue(session.hasMorePhotos)
        assertNull(session.current)
        assertTrue(session.loadNextBatch())
        assertEquals(201L, session.current?.id)
    }

    @Test
    fun `default batch contains 300 photos`() {
        val session = ReviewSession()
        session.load((1L..301L).map(::photo))

        assertEquals(300, session.snapshot().batchTotal)
        repeat(300) { assertTrue(session.keepCurrent()) }

        assertTrue(session.isBatchComplete)
        assertTrue(session.hasMorePhotos)
    }

    @Test
    fun `formatBytes uses readable binary units`() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("1.5 KB", formatBytes(1_536))
        assertEquals("2.0 MB", formatBytes(2L * 1024 * 1024))
    }

    private fun photo(id: Long, sizeBytes: Long = 1_024) =
        Photo(id = id, uri = "content://media/external/images/media/$id", displayName = "photo-$id.jpg", sizeBytes = sizeBytes)
}
