package com.kelvincalcano.cleanphotos.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kelvincalcano.cleanphotos.domain.PhotoAlbum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlbumSelectionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstAlbums_areDisplayedSideBySideWithAllPhotosFirst() {
        var selected: PhotoAlbum? = null
        val albums = listOf(
            PhotoAlbum.allPhotos(photoCount = 3, totalBytes = 3_000L),
            PhotoAlbum("camera", "Camera", null, 2, 2_000L),
            PhotoAlbum("screenshots", "Screenshots", null, 1, 1_000L),
        )

        composeRule.setContent {
            AlbumSelectionScreen(albums = albums, onAlbumSelected = { selected = it })
        }

        val allPhotos = composeRule
            .onNodeWithContentDescription("Álbum Todas las fotos")
            .fetchSemanticsNode()
            .boundsInRoot
        val camera = composeRule
            .onNodeWithContentDescription("Álbum Camera")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(allPhotos.top == camera.top)
        assertTrue(allPhotos.left < camera.left)

        composeRule.onNodeWithContentDescription("Álbum Todas las fotos").performClick()
        assertEquals("Todas las fotos", selected?.name)
    }
}
