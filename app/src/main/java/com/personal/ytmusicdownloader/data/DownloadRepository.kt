package com.personal.ytmusicdownloader.data

import com.personal.ytmusicdownloader.model.AudioFormat
import com.personal.ytmusicdownloader.model.DownloadProgress
import com.personal.ytmusicdownloader.model.TrackInfo
import kotlinx.coroutines.flow.Flow
import java.io.File

interface DownloadRepository {
    suspend fun fetchTrackInfo(url: String): Result<TrackInfo>
    fun downloadAudio(
        url: String,
        format: AudioFormat,
        targetDir: File
    ): Flow<DownloadProgress>
}

