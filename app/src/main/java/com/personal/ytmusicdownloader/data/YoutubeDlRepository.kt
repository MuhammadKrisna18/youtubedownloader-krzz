package com.personal.ytmusicdownloader.data

import com.personal.ytmusicdownloader.model.AudioFormat
import com.personal.ytmusicdownloader.model.DownloadProgress
import com.personal.ytmusicdownloader.model.TrackInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class YoutubeDlRepository(
    private val cookieStorage: CookieStorage
) : DownloadRepository {

    override suspend fun fetchTrackInfo(url: String): Result<TrackInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = YoutubeDLRequest(url).apply {
                addOption("--no-playlist")
                addOption("--skip-download")
                cookieStorage.getCookieFile()?.let { file ->
                    addOption("--cookies", file.absolutePath)
                }
            }
            val mediaInfo = YoutubeDL.getInstance().getInfo(request)
            TrackInfo(
                id = mediaInfo.id.orEmpty(),
                title = mediaInfo.title.orEmpty().ifEmpty { "Unknown Title" },
                uploader = mediaInfo.uploader.orEmpty().ifEmpty { "Unknown Artist" },
                durationSeconds = mediaInfo.duration.toLong(),
                thumbnailUrl = mediaInfo.thumbnail.orEmpty(),
                webUrl = mediaInfo.webpageUrl.orEmpty().ifEmpty { url }
            )
        }
    }

    override fun downloadAudio(
        url: String,
        format: AudioFormat,
        targetDir: File
    ): Flow<DownloadProgress> = callbackFlow {
        val processId = UUID.randomUUID().toString()
        trySend(DownloadProgress.Downloading(0f, "", ""))

        val request = YoutubeDLRequest(url).apply {
            addOption("-o", "${targetDir.absolutePath}/%(title)s.%(ext)s")
            addOption("-x")
            addOption("--audio-format", format.ytdlFormat)
            addOption("--audio-quality", "0")
            addOption("--add-metadata")
            addOption("--embed-thumbnail")
            addOption("--no-playlist")
            cookieStorage.getCookieFile()?.let { file ->
                addOption("--cookies", file.absolutePath)
            }
        }

        try {
            YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
                val speedMatch = SPEED_REGEX.find(line.orEmpty())?.value.orEmpty()
                val etaText = if (etaInSeconds > 0) "${etaInSeconds}s" else ""
                if (progress >= 100f) {
                    trySend(DownloadProgress.Transcoding)
                } else {
                    trySend(DownloadProgress.Downloading(progress, speedMatch, etaText))
                }
            }

            val downloadedFile = targetDir.listFiles()
                ?.filter { it.extension.equals(format.extension, ignoreCase = true) }
                ?.maxByOrNull { it.lastModified() }

            if (downloadedFile != null && downloadedFile.exists()) {
                trySend(DownloadProgress.Completed(downloadedFile.absolutePath, downloadedFile.name))
            } else {
                trySend(DownloadProgress.Failed("Downloaded audio file not found"))
            }
            close()
        } catch (e: Exception) {
            trySend(DownloadProgress.Failed(e.localizedMessage ?: "Download failed"))
            close(e)
        }

        awaitClose {
            runCatching {
                YoutubeDL.getInstance().destroyProcessById(processId)
            }
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private val SPEED_REGEX = """\b\d+(\.\d+)?[kKMGT]?i?B/s\b""".toRegex()
    }
}

