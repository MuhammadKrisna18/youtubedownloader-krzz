package com.personal.ytmusicdownloader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.personal.ytmusicdownloader.R
import com.personal.ytmusicdownloader.YTDownloaderApp
import com.personal.ytmusicdownloader.model.AudioFormat
import com.personal.ytmusicdownloader.model.DownloadProgress
import com.personal.ytmusicdownloader.model.TrackInfo
import com.personal.ytmusicdownloader.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var downloadJob: Job? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifEmpty { "Audio Track" }
                val uploader = intent.getStringExtra(EXTRA_ARTIST).orEmpty().ifEmpty { "Unknown Artist" }
                val formatName = intent.getStringExtra(EXTRA_FORMAT) ?: AudioFormat.MP3.name
                val format = runCatching { AudioFormat.valueOf(formatName) }.getOrDefault(AudioFormat.MP3)
                val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
                val thumbnail = intent.getStringExtra(EXTRA_THUMBNAIL).orEmpty()

                val trackInfo = TrackInfo(
                    id = "",
                    title = title,
                    uploader = uploader,
                    durationSeconds = duration,
                    thumbnailUrl = thumbnail,
                    webUrl = url
                )

                startDownload(url, trackInfo, format)
            }
            ACTION_CANCEL_DOWNLOAD -> {
                cancelDownload()
            }
        }
        return START_NOT_STICKY
    }

    private fun startDownload(url: String, trackInfo: TrackInfo, format: AudioFormat) {
        downloadJob?.cancel()

        val initialNotification = buildProgressNotification(
            title = trackInfo.title,
            contentText = "Starting download...",
            progress = 0,
            isIndeterminate = true
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        downloadJob = serviceScope.launch {
            val app = application as YTDownloaderApp
            val repository = app.container.downloadRepository
            val mediaManager = app.container.mediaStorageManager
            val tempDir = File(cacheDir, "temp_downloads").apply { if (!exists()) mkdirs() }

            repository.downloadAudio(url, format, tempDir).collect { progress ->
                _downloadState.value = progress
                when (progress) {
                    is DownloadProgress.Downloading -> {
                        val progressInt = progress.progressPercent.toInt().coerceAtLeast(0)
                        val isIndeterminate = progress.progressPercent <= 0f
                        val info = if (isIndeterminate) {
                            "Menyiapkan unduhan..."
                        } else {
                            val speedPart = if (progress.speedText.isNotBlank()) " • ${progress.speedText}" else ""
                            val etaPart = if (progress.etaText.isNotBlank()) " • ETA ${progress.etaText}" else ""
                            "${progressInt}%$speedPart$etaPart"
                        }
                        updateNotification(
                            buildProgressNotification(
                                title = trackInfo.title,
                                contentText = info,
                                progress = progressInt,
                                isIndeterminate = isIndeterminate
                            )
                        )
                    }
                    is DownloadProgress.Transcoding -> {
                        updateNotification(
                            buildProgressNotification(
                                title = trackInfo.title,
                                contentText = "Mengonversi audio & menyematkan cover...",
                                progress = 100,
                                isIndeterminate = true
                            )
                        )
                    }
                    is DownloadProgress.Completed -> {
                        val exportedUri = mediaManager.exportToPublicMusic(
                            sourceFile = File(progress.filePath),
                            trackInfo = trackInfo,
                            format = format
                        )
                        val finalProgress = exportedUri.fold(
                            onSuccess = { uri ->
                                showFinalNotification(
                                    title = "Download Complete",
                                    contentText = "${trackInfo.title} saved to Music"
                                )
                                DownloadProgress.Completed(uri.toString(), progress.fileName)
                            },
                            onFailure = { error ->
                                showFinalNotification(
                                    title = "Export Failed",
                                    contentText = error.localizedMessage ?: "Failed to save file"
                                )
                                DownloadProgress.Failed(error.localizedMessage ?: "Failed to save file")
                            }
                        )
                        _downloadState.value = finalProgress
                        stopForegroundGracefully()
                    }
                    is DownloadProgress.Failed -> {
                        showFinalNotification(
                            title = "Download Failed",
                            contentText = progress.message
                        )
                        stopForegroundGracefully()
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = DownloadProgress.Idle
        stopForegroundGracefully()
    }

    private fun stopForegroundGracefully() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
        stopSelf()
    }

    private fun buildProgressNotification(
        title: String,
        contentText: String,
        progress: Int,
        isIndeterminate: Boolean
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).let { intent ->
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
        }.let { intent ->
            PendingIntent.getService(
                this,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, isIndeterminate)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()
    }

    private fun showFinalNotification(title: String, contentText: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).let { intent ->
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_download)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .build()

        notificationManager.notify(COMPLETED_NOTIFICATION_ID, notification)
    }

    private fun updateNotification(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download and transcoding progress"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_DOWNLOAD = "com.personal.ytmusicdownloader.action.START"
        const val ACTION_CANCEL_DOWNLOAD = "com.personal.ytmusicdownloader.action.CANCEL"

        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_FORMAT = "extra_format"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_THUMBNAIL = "extra_thumbnail"

        private const val CHANNEL_ID = "yt_music_download_channel"
        private const val NOTIFICATION_ID = 1001
        private const val COMPLETED_NOTIFICATION_ID = 1002

        private val _downloadState = MutableStateFlow<DownloadProgress>(DownloadProgress.Idle)
        val downloadState: StateFlow<DownloadProgress> = _downloadState.asStateFlow()

        fun start(
            context: Context,
            url: String,
            trackInfo: TrackInfo,
            format: AudioFormat
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, trackInfo.title)
                putExtra(EXTRA_ARTIST, trackInfo.uploader)
                putExtra(EXTRA_FORMAT, format.name)
                putExtra(EXTRA_DURATION, trackInfo.durationSeconds)
                putExtra(EXTRA_THUMBNAIL, trackInfo.thumbnailUrl)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancel(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
            }
            context.startService(intent)
        }
    }
}

