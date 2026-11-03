package com.personal.ytmusicdownloader.model

sealed interface DownloadProgress {
    data object Idle : DownloadProgress
    data object FetchingInfo : DownloadProgress
    data class Downloading(
        val progressPercent: Float,
        val speedText: String,
        val etaText: String
    ) : DownloadProgress
    data object Transcoding : DownloadProgress
    data class Completed(
        val filePath: String,
        val fileName: String
    ) : DownloadProgress
    data class Failed(
        val message: String
    ) : DownloadProgress
}

