package com.kelvincalcano.cleanphotos.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
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

    @Test
    fun sortMenu_exposesTheSixAlbumOrders() {
        composeRule.setContent {
            AlbumSelectionScreen(
                albums = listOf(
                    PhotoAlbum.allPhotos(photoCount = 3, totalBytes = 3_000L),
                    PhotoAlbum("camera", "Camera", null, 2, 2_000L),
                ),
                onAlbumSelected = {},
            )
        }

        composeRule
            .onNodeWithContentDescription("Orden actual: Nombre: A a Z")
            .performClick()

        composeRule.onNodeWithText("Tamaño: mayor a menor").assertIsDisplayed()
        composeRule.onNodeWithText("Tamaño: menor a mayor").assertIsDisplayed()
        assertEquals(
            2,
            composeRule.onAllNodesWithText("Nombre: A a Z").fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithText("Nombre: Z a A").assertIsDisplayed()
        composeRule.onNodeWithText("Foto más reciente primero").assertIsDisplayed()
        composeRule.onNodeWithText("Foto más antigua primero").assertIsDisplayed()

        composeRule.onNodeWithText("Tamaño: mayor a menor").performClick()
        composeRule
            .onNodeWithContentDescription("Orden actual: Tamaño: mayor a menor")
            .assertIsDisplayed()
    }

    @Test
    fun photoSortMenu_exposesDateAndSizeOrders() {
        val selectedOrder = androidx.compose.runtime.mutableStateOf(
            com.kelvincalcano.cleanphotos.domain.PhotoSortOrder.DATE_ASC,
        )

        composeRule.setContent {
            AlbumSelectionScreen(
                albums = listOf(
                    PhotoAlbum.allPhotos(photoCount = 3, totalBytes = 3_000L),
                ),
                onAlbumSelected = {},
                photoSortOrder = selectedOrder.value,
                onPhotoSortOrderChanged = { selectedOrder.value = it },
            )
        }

        composeRule
            .onNodeWithContentDescription("Orden actual de fotos: Fecha: más antigua primero")
            .performClick()

        assertTrue(
            composeRule.onAllNodesWithText("Fecha: más antigua primero")
                .fetchSemanticsNodes()
                .size >= 2,
        )
        composeRule.onNodeWithText("Fecha: más reciente primero").assertIsDisplayed()
        composeRule.onNodeWithText("Tamaño: mayor a menor").assertIsDisplayed()
        composeRule.onNodeWithText("Tamaño: menor a mayor").assertIsDisplayed()

        composeRule.onNodeWithText("Tamaño: mayor a menor").performClick()
        assertEquals(
            com.kelvincalcano.cleanphotos.domain.PhotoSortOrder.SIZE_DESC,
            selectedOrder.value,
        )
    }
}
