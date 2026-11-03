package com.personal.ytmusicdownloader.data

import android.content.Context
import com.personal.ytmusicdownloader.model.AudioFormat
import com.personal.ytmusicdownloader.model.DownloadProgress
import com.personal.ytmusicdownloader.model.TrackInfo
import com.yausername.ffmpeg.FFmpeg
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
    private val context: Context,
    private val cookieStorage: CookieStorage
) : DownloadRepository {

    private fun ensureInitialized(): Result<Unit> = runCatching {
        YoutubeDL.getInstance().init(context)
        FFmpeg.getInstance().init(context)
    }

    override suspend fun fetchTrackInfo(url: String): Result<TrackInfo> = withContext(Dispatchers.IO) {
        val initResult = ensureInitialized()
        if (initResult.isFailure) {
            return@withContext Result.failure(
                initResult.exceptionOrNull() ?: IllegalStateException("Gagal inisialisasi mesin yt-dlp")
            )
        }

        runCatching {
            val request = YoutubeDLRequest(url).apply {
                addOption("--no-playlist")
                addOption("--skip-download")
                addOption("--no-check-certificates")
                addOption("--prefer-free-formats")
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
        val initResult = ensureInitialized()
        if (initResult.isFailure) {
            val errorMsg = initResult.exceptionOrNull()?.localizedMessage ?: "Inisialisasi yt-dlp gagal"
            trySend(DownloadProgress.Failed(errorMsg))
            close()
            return@callbackFlow
        }

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
            addOption("--no-check-certificates")
            addOption("--newline")
            addOption("--progress")
            cookieStorage.getCookieFile()?.let { file ->
                addOption("--cookies", file.absolutePath)
            }
        }

        try {
            var lastProgress = 0f
            YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
                val lineText = line.orEmpty().trim()
                val isTranscodingLine = TRANSCODING_KEYWORDS.any { lineText.contains(it, ignoreCase = true) }

                if (isTranscodingLine || progress >= 100f) {
                    trySend(DownloadProgress.Transcoding)
                    return@execute
                }

                val parsedPercent = PROGRESS_REGEX.find(lineText)?.groupValues?.getOrNull(1)?.toFloatOrNull()
                val currentPercent = when {
                    progress >= 0f -> progress
                    parsedPercent != null -> parsedPercent
                    else -> null
                }

                if (currentPercent != null) {
                    if (currentPercent >= 100f) {
                        trySend(DownloadProgress.Transcoding)
                    } else {
                        lastProgress = currentPercent
                        val speedMatch = SPEED_REGEX.find(lineText)?.value.orEmpty()
                        val etaMatch = if (etaInSeconds > 0) "${etaInSeconds}s" else ETA_REGEX.find(lineText)?.groupValues?.getOrNull(1).orEmpty()
                        trySend(DownloadProgress.Downloading(currentPercent.coerceIn(0f, 99.9f), speedMatch, etaMatch))
                    }
                } else if (lastProgress > 0f) {
                    val speedMatch = SPEED_REGEX.find(lineText)?.value.orEmpty()
                    val etaMatch = if (etaInSeconds > 0) "${etaInSeconds}s" else ETA_REGEX.find(lineText)?.groupValues?.getOrNull(1).orEmpty()
                    trySend(DownloadProgress.Downloading(lastProgress.coerceIn(0f, 99.9f), speedMatch, etaMatch))
                } else {
                    trySend(DownloadProgress.Downloading(0f, "", ""))
                }
            }

            val downloadedFile = targetDir.listFiles()
                ?.filter { it.extension.equals(format.extension, ignoreCase = true) }
                ?.maxByOrNull { it.lastModified() }

            if (downloadedFile != null && downloadedFile.exists()) {
                trySend(DownloadProgress.Completed(downloadedFile.absolutePath, downloadedFile.name))
            } else {
                trySend(DownloadProgress.Failed("File audio tidak ditemukan setelah pengunduhan"))
            }
            close()
        } catch (e: Exception) {
            trySend(DownloadProgress.Failed(e.localizedMessage ?: "Gagal mengunduh audio"))
            close(e)
        }

        awaitClose {
            runCatching {
                YoutubeDL.getInstance().destroyProcessById(processId)
            }
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private val PROGRESS_REGEX = """\[download\]\s+(\d+(?:\.\d+)?)%""".toRegex()
        private val SPEED_REGEX = """\b\d+(?:\.\d+)?\s*[kKMGT]?i?B/s\b""".toRegex()
        private val ETA_REGEX = """ETA\s+([0-9:]+)""".toRegex()
        private val TRANSCODING_KEYWORDS = listOf(
            "[ExtractAudio]",
            "[ffmpeg]",
            "[Metadata]",
            "[ThumbnailsConvertor]",
            "[EmbedSubtitle]",
            "[FixupM4a]"
        )
    }
}
