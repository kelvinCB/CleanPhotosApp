package com.kelvincalcano.cleanphotos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeptPhotoStoreTest {
    @Test
    fun `kept photos stay excluded after recreating the store`() {
        var persistedUris = emptySet<String>()
        val firstSession = KeptPhotoStore(persistedUris) { persistedUris = it }
        val kept = photo(1)

        assertTrue(firstSession.markKept(kept))

        val restartedStore = KeptPhotoStore(persistedUris) { persistedUris = it }

        assertEquals(listOf(photo(2)), restartedStore.filterUnreviewed(listOf(kept, photo(2))))
        assertTrue(restartedStore.isKept(kept))
    }

    @Test
    fun `marking the same photo twice does not duplicate persisted entries`() {
        var persistedUris = emptySet<String>()
        val store = KeptPhotoStore(persistedUris) { persistedUris = it }
        val kept = photo(1)

        assertTrue(store.markKept(kept))
        assertTrue(!store.markKept(kept))

        assertEquals(setOf(kept.uri), persistedUris)
    }

    @Test
    fun `unmarking a kept photo removes it from persisted entries`() {
        var persistedUris = emptySet<String>()
        val store = KeptPhotoStore(persistedUris) { persistedUris = it }
        val kept = photo(1)

        store.markKept(kept)

        assertTrue(store.unmarkKept(kept))
        assertEquals(emptySet<String>(), persistedUris)
        assertTrue(!store.isKept(kept))
    }

    private fun photo(id: Long) = Photo(
        id = id,
        uri = "content://media/external/images/media/$id",
        displayName = "photo-$id.jpg",
        sizeBytes = 1_024L,
    )
}
