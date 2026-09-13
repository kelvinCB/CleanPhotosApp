package com.kelvincalcano.cleanphotos.domain

data class Photo(
    val id: Long,
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
)

fun formatBytes(bytes: Long): String {
    if (bytes < 1_024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1_024 && unitIndex < units.lastIndex) {
        value /= 1_024
        unitIndex++
    }
    return "%.1f %s".format(java.util.Locale.US, value, units[unitIndex])
}

