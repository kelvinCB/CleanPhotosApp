package com.kelvincalcano.cleanphotos.domain

/**
 * Keeps the UI snapshot synchronized with every review decision.
 * ReviewSession is intentionally UI-agnostic, so this holder publishes a new
 * immutable snapshot after each state-changing operation.
 */
class ReviewStateHolder(
    private val session: ReviewSession = ReviewSession(),
) {
    var state: ReviewSessionSnapshot = session.snapshot()
        private set

    fun load(photos: List<Photo>) {
        session.load(photos)
        publish()
    }

    fun keepCurrent(): Boolean = session.keepCurrent().also { changed ->
        if (changed) publish()
    }

    fun requestTrashCurrent(): Photo? = session.requestTrashCurrent().also { photo ->
        if (photo != null) publish()
    }

    fun confirmTrash(photoId: Long): Boolean = session.confirmTrash(photoId).also { changed ->
        if (changed) publish()
    }

    fun cancelTrash(photoId: Long): Boolean = session.cancelTrash(photoId).also { changed ->
        if (changed) publish()
    }

    fun loadNextBatch(): Boolean = session.loadNextBatch().also { changed ->
        if (changed) publish()
    }

    private fun publish() {
        state = session.snapshot()
    }
}
