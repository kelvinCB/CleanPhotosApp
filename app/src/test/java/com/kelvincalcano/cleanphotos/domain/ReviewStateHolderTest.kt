package com.kelvincalcano.cleanphotos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewStateHolderTest {
    @Test
    fun `two consecutive keeps publish the next screen state`() {
        val holder = ReviewStateHolder(ReviewSession(batchSize = 2))
        holder.load(listOf(photo(1), photo(2)))

        assertEquals(1L, holder.state.current?.id)
        assertTrue(holder.keepCurrent())
        assertEquals(2L, holder.state.current?.id)
        assertTrue(holder.keepCurrent())
        assertTrue(holder.state.isBatchComplete)
        assertEquals(null, holder.state.current)
    }

    private fun photo(id: Long) = Photo(
        id = id,
        uri = "content://media/external/images/media/$id",
        displayName = "photo-$id.jpg",
        sizeBytes = 1_024L,
    )
}
