package com.kelvincalcano.cleanphotos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchWritePromptControllerTest {
    @Test
    fun `batch access explains before starting native request`() {
        val controller = BatchWritePromptController()
        val uris = listOf("content://media/1", "content://media/2")

        assertTrue(controller.prepare("first-batch", uris))
        assertTrue(controller.isExplanationVisible)
        assertFalse(controller.isNativeRequestStarted)

        assertEquals(uris, controller.acknowledgeExplanation())
        assertFalse(controller.isExplanationVisible)
        assertTrue(controller.isNativeRequestStarted)
    }
}
