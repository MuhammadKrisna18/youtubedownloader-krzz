package com.personal.ytmusicdownloader.model

data class CookieInfo(
    val exists: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val entryCount: Int = 0
)
