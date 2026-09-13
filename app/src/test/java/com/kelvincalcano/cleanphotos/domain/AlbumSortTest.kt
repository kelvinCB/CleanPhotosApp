package com.kelvincalcano.cleanphotos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumSortTest {
    private val allPhotos = PhotoAlbum.allPhotos(
        photoCount = 100,
        totalBytes = 50_000L,
        coverUri = "content://all",
    ).copy(latestPhotoDateAdded = 10L)

    private val camera = PhotoAlbum(
        id = "camera",
        name = "Camera",
        coverUri = null,
        photoCount = 10,
        totalBytes = 10_000L,
        latestPhotoDateAdded = 20L,
    )

    private val screenshots = PhotoAlbum(
        id = "screenshots",
        name = "Screenshots",
        coverUri = null,
        photoCount = 20,
        totalBytes = 30_000L,
        latestPhotoDateAdded = 30L,
    )

    @Test
    fun `every sort keeps all photos first and orders remaining albums by size`() {
        val albums = listOf(screenshots, allPhotos, camera)

        assertEquals(
            listOf(allPhotos, screenshots, camera),
            sortAlbums(albums, AlbumSortOrder.SIZE_DESC),
        )
        assertEquals(
            listOf(allPhotos, camera, screenshots),
            sortAlbums(albums, AlbumSortOrder.SIZE_ASC),
        )
    }

    @Test
    fun `alphabetical sort supports both directions`() {
        val albums = listOf(screenshots, allPhotos, camera)

        assertEquals(
            listOf(allPhotos, camera, screenshots),
            sortAlbums(albums, AlbumSortOrder.NAME_ASC),
        )
        assertEquals(
            listOf(allPhotos, screenshots, camera),
            sortAlbums(albums, AlbumSortOrder.NAME_DESC),
        )
    }

    @Test
    fun `recent photo sort supports both directions`() {
        val albums = listOf(screenshots, allPhotos, camera)

        assertEquals(
            listOf(allPhotos, screenshots, camera),
            sortAlbums(albums, AlbumSortOrder.RECENT_DESC),
        )
        assertEquals(
            listOf(allPhotos, camera, screenshots),
            sortAlbums(albums, AlbumSortOrder.RECENT_ASC),
        )
    }
}
