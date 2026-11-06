package com.personal.ytmusicdownloader.ui

import com.personal.ytmusicdownloader.model.AppUpdateInfo
import com.personal.ytmusicdownloader.model.AudioFormat
import com.personal.ytmusicdownloader.model.DownloadProgress
import com.personal.ytmusicdownloader.model.TrackInfo

data class MainUiState(
    val url: String = "",
    val isLoadingInfo: Boolean = false,
    val trackInfo: TrackInfo? = null,
    val selectedFormat: AudioFormat = AudioFormat.MP3,
    val downloadProgress: DownloadProgress = DownloadProgress.Idle,
    val updateInfo: AppUpdateInfo? = null,
    val isCheckingUpdate: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val updateProgress: Float = 0f,
    val userMessage: String? = null,
    val errorMessage: String? = null,
    val showUpdateDialog: Boolean = false
)
