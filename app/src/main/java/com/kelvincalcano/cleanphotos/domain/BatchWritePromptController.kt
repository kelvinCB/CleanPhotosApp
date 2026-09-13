package com.kelvincalcano.cleanphotos.domain

/** Coordinates the app explanation and Android's native batch-write request. */
class BatchWritePromptController {
    private var preparedBatchKey: String? = null
    private var pendingUris: List<String> = emptyList()

    var isExplanationVisible: Boolean = false
        private set

    var isNativeRequestStarted: Boolean = false
        private set

    fun prepare(batchKey: String, missingUris: List<String>): Boolean {
        if (missingUris.isEmpty() || batchKey == preparedBatchKey) return false

        preparedBatchKey = batchKey
        pendingUris = missingUris.distinct()
        isExplanationVisible = true
        isNativeRequestStarted = false
        return true
    }

    fun acknowledgeExplanation(): List<String> {
        check(isExplanationVisible) { "No batch-write explanation is pending" }
        isExplanationVisible = false
        isNativeRequestStarted = true
        return pendingUris
    }

    fun finishNativeRequest() {
        pendingUris = emptyList()
        isNativeRequestStarted = false
    }

    fun dismissExplanation() {
        isExplanationVisible = false
        isNativeRequestStarted = false
        pendingUris = emptyList()
    }
}
