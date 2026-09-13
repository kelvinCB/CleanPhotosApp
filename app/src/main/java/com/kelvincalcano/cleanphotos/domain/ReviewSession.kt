package com.kelvincalcano.cleanphotos.domain

class ReviewSession(private val batchSize: Int = DEFAULT_BATCH_SIZE) {
    init {
        require(batchSize > 0) { "batchSize must be positive" }
    }

    private var photos: List<Photo> = emptyList()
    private val decisions = mutableListOf<UndoableDecision>()
    private var unlockedPhotoCount: Int = 0
    private var batchStartIndex: Int = 0
    private var pendingTrash: Photo? = null

    val current: Photo?
        get() = photos.getOrNull(decisions.size)
            ?.takeIf { decisions.size < unlockedPhotoCount }

    val pendingTrashPhoto: Photo?
        get() = pendingTrash

    val currentBatch: List<Photo>
        get() = photos.drop(batchStartIndex).take(batchSize)

    val keptCount: Int
        get() = currentBatchDecisions.count { it.decision == PhotoDecision.KEEP }

    val trashedCount: Int
        get() = currentBatchDecisions.count { it.decision == PhotoDecision.TRASH }

    val trashedBytes: Long
        get() = currentBatchDecisions
            .filter { it.decision == PhotoDecision.TRASH }
            .sumOf { it.photo.sizeBytes.coerceAtLeast(0L) }

    val reviewedCount: Int
        get() = currentBatchDecisions.size

    val isBatchComplete: Boolean
        get() = photos.isNotEmpty() && current == null && pendingTrash == null

    val hasMorePhotos: Boolean
        get() = decisions.size < photos.size

    val hasPhotos: Boolean
        get() = photos.isNotEmpty()

    val canUndo: Boolean
        get() = decisions.lastOrNull()?.decision == PhotoDecision.KEEP && pendingTrash == null

    fun lastDecision(): UndoableDecision? = decisions.lastOrNull()

    fun snapshot() = ReviewSessionSnapshot(
        current = current,
        pendingTrash = pendingTrash != null,
        reviewedCount = reviewedCount,
        keptCount = keptCount,
        trashedCount = trashedCount,
        trashedBytes = trashedBytes,
        batchSize = batchSize,
        batchTotal = currentBatch.size,
        isBatchComplete = isBatchComplete,
        hasMorePhotos = hasMorePhotos,
        hasPhotos = hasPhotos,
        previousPhotos = decisions
            .asReversed()
            .filter { it.decision == PhotoDecision.KEEP }
            .take(PREVIOUS_PHOTO_LIMIT)
            .map(UndoableDecision::photo),
        canUndo = canUndo,
    )

    fun load(photos: List<Photo>) {
        this.photos = photos
        decisions.clear()
        pendingTrash = null
        batchStartIndex = 0
        unlockedPhotoCount = minOf(batchSize, photos.size)
    }

    fun loadNextBatch(): Boolean {
        if (pendingTrash != null || current != null || !hasMorePhotos) return false
        batchStartIndex = unlockedPhotoCount
        unlockedPhotoCount = minOf(unlockedPhotoCount + batchSize, photos.size)
        return true
    }

    fun keepCurrent(): Boolean {
        if (current == null || pendingTrash != null) return false
        return recordDecision(PhotoDecision.KEEP)
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
        return recordDecision(PhotoDecision.TRASH)
    }

    fun cancelTrash(photoId: Long): Boolean {
        val photo = pendingTrash ?: return false
        if (photo.id != photoId) return false
        pendingTrash = null
        return true
    }

    fun undoLastDecision(): UndoableDecision? {
        if (!canUndo) return null
        val undone = decisions.removeAt(decisions.lastIndex)
        while (batchStartIndex > decisions.size) {
            batchStartIndex = (batchStartIndex - batchSize).coerceAtLeast(0)
        }
        return undone
    }

    private fun recordDecision(decision: PhotoDecision): Boolean {
        val photo = current ?: return false
        decisions += UndoableDecision(photo, decision)
        if (decisions.size >= batchStartIndex + batchSize &&
            unlockedPhotoCount > batchStartIndex + batchSize
        ) {
            batchStartIndex += batchSize
        }
        return true
    }

    private val currentBatchDecisions: List<UndoableDecision>
        get() = decisions.drop(batchStartIndex).take(batchSize)

    private companion object {
        const val DEFAULT_BATCH_SIZE = 300
        const val PREVIOUS_PHOTO_LIMIT = 3
    }
}

enum class PhotoDecision {
    KEEP,
    TRASH,
}

data class UndoableDecision(
    val photo: Photo,
    val decision: PhotoDecision,
)

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
    val previousPhotos: List<Photo> = emptyList(),
    val canUndo: Boolean = false,
)
