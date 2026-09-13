package com.kelvincalcano.cleanphotos.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kelvincalcano.cleanphotos.domain.Photo
import com.kelvincalcano.cleanphotos.domain.ReviewSession
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reviewScreen_exposesAccessibleKeepAndTrashActions() {
        val session = ReviewSession(batchSize = 1).apply {
            load(listOf(Photo(1L, "content://test/1", "camera.jpg", 12_000L)))
        }

        composeRule.setContent {
            ReviewScreen(
                state = session.snapshot(),
                onKeep = {},
                onTrash = {},
                onLoadMore = {},
            )
        }

        composeRule.onNodeWithContentDescription("Foto camera.jpg").fetchSemanticsNode()
        composeRule.onNodeWithText("Conservar").fetchSemanticsNode()
        composeRule.onNodeWithText("Papelera").fetchSemanticsNode()
    }

    @Test
    fun reviewScreen_clickingKeepCallsCallback() {
        val session = ReviewSession(batchSize = 1).apply {
            load(listOf(Photo(1L, "content://test/1", "camera.jpg", 12_000L)))
        }
        var kept = false

        composeRule.setContent {
            ReviewScreen(
                state = session.snapshot(),
                onKeep = { kept = true },
                onTrash = {},
                onLoadMore = {},
            )
        }

        composeRule.onNodeWithText("Conservar").performClick()

        assert(kept)
    }
}
