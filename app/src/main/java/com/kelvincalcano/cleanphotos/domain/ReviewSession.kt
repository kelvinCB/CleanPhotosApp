package com.kelvincalcano.cleanphotos.domain

class ReviewSession(private val batchSize: Int = DEFAULT_BATCH_SIZE) {
    init {
        require(batchSize > 0) { "batchSize must be positive" }
    }

    private var remainingPhotos: List<Photo> = emptyList()
    private var activeBatch: List<Photo> = emptyList()
    private var currentIndex: Int = 0
    private var pendingTrash: Photo? = null

    var keptCount: Int = 0
        private set
    var trashedCount: Int = 0
        private set
    var trashedBytes: Long = 0
        private set

    val current: Photo?
        get() = activeBatch.getOrNull(currentIndex)

    val pendingTrashPhoto: Photo?
        get() = pendingTrash

    val reviewedCount: Int
        get() = keptCount + trashedCount

    val isBatchComplete: Boolean
        get() = activeBatch.isNotEmpty() && current == null && pendingTrash == null

    val hasMorePhotos: Boolean
        get() = remainingPhotos.isNotEmpty()

    val hasPhotos: Boolean
        get() = activeBatch.isNotEmpty() || remainingPhotos.isNotEmpty()

    fun snapshot() = ReviewSessionSnapshot(
        current = current,
        pendingTrash = pendingTrash != null,
        reviewedCount = reviewedCount,
        keptCount = keptCount,
        trashedCount = trashedCount,
        trashedBytes = trashedBytes,
        batchSize = batchSize,
        batchTotal = activeBatch.size,
        isBatchComplete = isBatchComplete,
        hasMorePhotos = hasMorePhotos,
        hasPhotos = hasPhotos,
    )

    fun load(photos: List<Photo>) {
        remainingPhotos = photos
        resetBatchStats()
        loadNextBatch()
    }

    fun loadNextBatch(): Boolean {
        if (pendingTrash != null || (current != null && !isBatchComplete)) return false
        if (remainingPhotos.isEmpty()) {
            activeBatch = emptyList()
            currentIndex = 0
            return false
        }
        activeBatch = remainingPhotos.take(batchSize)
        remainingPhotos = remainingPhotos.drop(activeBatch.size)
        currentIndex = 0
        pendingTrash = null
        resetBatchStats()
        return true
    }

    fun keepCurrent(): Boolean {
        if (current == null || pendingTrash != null) return false
        keptCount++
        currentIndex++
        return true
    }

    fun requestTrashCurrent(): Photo? {
        if (pendingTrash != null) return null
        pendingTrash = current
        return pendingTrash
    }

    fun confirmTrash(photoId: Long): Boolean {
        val photo = pendingTrash ?: return false
        if (photo.id != photoId) return false
        pendingTrash = null
        trashedCount++
        trashedBytes += photo.sizeBytes.coerceAtLeast(0)
        currentIndex++
        return true
    }

    fun cancelTrash(photoId: Long): Boolean {
        val photo = pendingTrash ?: return false
        if (photo.id != photoId) return false
        pendingTrash = null
        return true
    }

    private fun resetBatchStats() {
        keptCount = 0
        trashedCount = 0
        trashedBytes = 0
    }

    private companion object {
        const val DEFAULT_BATCH_SIZE = 200
    }
}

data class ReviewSessionSnapshot(
    val current: Photo?,
    val pendingTrash: Boolean,
    val reviewedCount: Int,
    val keptCount: Int,
    val trashedCount: Int,
    val trashedBytes: Long,
    val batchSize: Int,
    val batchTotal: Int,
    val isBatchComplete: Boolean,
    val hasMorePhotos: Boolean,
    val hasPhotos: Boolean,
)
