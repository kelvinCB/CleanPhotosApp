package com.kelvincalcano.cleanphotos.ui

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasScrollAction
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

    @Test
    fun completedShortAlbum_exposesChooseAnotherAlbumAction() {
        val session = ReviewSession(batchSize = 300).apply {
            load(listOf(Photo(1L, "content://test/1", "camera.jpg", 12_000L)))
            keepCurrent()
        }
        var changedAlbum = false

        composeRule.setContent {
            ReviewScreen(
                state = session.snapshot(),
                onKeep = {},
                onTrash = {},
                onLoadMore = {},
                onChangeAlbum = { changedAlbum = true },
            )
        }

        composeRule.onNodeWithText("Elegir otro álbum").performClick()

        assert(changedAlbum)
    }

    @Test
    fun reviewScreen_showsPreviousPhotoAndRecoversIt() {
        val session = ReviewSession(batchSize = 2).apply {
            load(
                listOf(
                    Photo(1L, "content://test/1", "first.jpg", 12_000L),
                    Photo(2L, "content://test/2", "second.jpg", 12_000L),
                ),
            )
            keepCurrent()
        }
        var recovered = false

        composeRule.setContent {
            ReviewScreen(
                state = session.snapshot(),
                onKeep = {},
                onTrash = {},
                onLoadMore = {},
                onUndo = { recovered = true },
            )
        }

        val previousBounds = composeRule
            .onNodeWithContentDescription("Foto anterior first.jpg")
            .fetchSemanticsNode()
            .boundsInRoot
        val currentBounds = composeRule
            .onNodeWithContentDescription("Foto second.jpg")
            .fetchSemanticsNode()
            .boundsInRoot
        assert(previousBounds.bottom <= currentBounds.top)
        composeRule.onNodeWithText("Recuperar foto").performClick()

        assert(recovered)
    }

    @Test
    fun reviewScreen_hidesRecoveryAfterTrashDecision() {
        val session = ReviewSession(batchSize = 2).apply {
            load(
                listOf(
                    Photo(1L, "content://test/1", "first.jpg", 12_000L),
                    Photo(2L, "content://test/2", "second.jpg", 12_000L),
                ),
            )
            requestTrashCurrent()
            confirmTrash(1L)
        }

        composeRule.setContent {
            ReviewScreen(
                state = session.snapshot(),
                onKeep = {},
                onTrash = {},
                onLoadMore = {},
            )
        }

        assert(composeRule.onAllNodesWithText("Recuperar foto").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun portraitReview_allowsVerticalScrollingForBottomFeedback() {
        val session = ReviewSession(batchSize = 1).apply {
            load(listOf(Photo(1L, "content://test/1", "camera.jpg", 12_000L)))
        }

        composeRule.setContent {
            ReviewScreen(
                state = session.snapshot(),
                onKeep = {},
                onTrash = {},
                onLoadMore = {},
                onUndo = {},
                feedback = "Foto recuperada",
            )
        }

        composeRule
            .onNodeWithContentDescription("Contenido desplazable de revisión")
            .assert(hasScrollAction())
    }

    @Test
    fun landscapeReview_usesAHorizontalLayout() {
        val session = ReviewSession(batchSize = 1).apply {
            load(listOf(Photo(1L, "content://test/1", "camera.jpg", 12_000L)))
        }
        val landscapeConfiguration = Configuration().apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        }

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides landscapeConfiguration) {
                ReviewScreen(
                    state = session.snapshot(),
                    onKeep = {},
                    onTrash = {},
                    onLoadMore = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Revisión en horizontal")
            .fetchSemanticsNode()
        composeRule.onNodeWithText("Conservar").fetchSemanticsNode()
        composeRule.onNodeWithText("Papelera").fetchSemanticsNode()
    }

    @Test
    fun completedAlbum_explainsThatAllPhotosWereAlreadyMarked() {
        val session = ReviewSession(batchSize = 300).apply {
            load(emptyList())
        }
        var changedAlbum = false
        var reactivated = false

        composeRule.setContent {
            ReviewScreen(
                state = session.snapshot(),
                onKeep = {},
                onTrash = {},
                onLoadMore = {},
                albumName = "WhatsApp Documents",
                albumPhotoCount = 2,
                onChangeAlbum = { changedAlbum = true },
                onReactivatePhotos = { reactivated = true },
            )
        }

        composeRule.onNodeWithText(
            "Las 2 fotos de WhatsApp Documents ya están marcadas como conservadas.",
        ).fetchSemanticsNode()
        assert(
            composeRule.onAllNodesWithText(
                "Clean Photos no volverá a mostrarlas en una nueva sesión.",
            ).fetchSemanticsNodes().isEmpty(),
        )
        composeRule.onNodeWithText("Elegir otro álbum").performClick()
        composeRule.onNodeWithText("Volver a activar fotos para clasificarlas").performClick()

        assert(changedAlbum)
        assert(reactivated)
    }
}
