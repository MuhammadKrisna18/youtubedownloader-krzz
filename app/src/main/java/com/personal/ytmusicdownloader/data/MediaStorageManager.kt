package com.personal.ytmusicdownloader.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.personal.ytmusicdownloader.model.AudioFormat
import com.personal.ytmusicdownloader.model.TrackInfo
import java.io.File
import java.io.FileInputStream

class MediaStorageManager(private val context: Context) {

    fun exportToPublicMusic(
        sourceFile: File,
        trackInfo: TrackInfo,
        format: AudioFormat
    ): Result<Uri> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(sourceFile, trackInfo, format)
        } else {
            saveViaLegacyStorage(sourceFile, trackInfo, format)
        }
    }

    private fun saveViaMediaStore(
        sourceFile: File,
        trackInfo: TrackInfo,
        format: AudioFormat
    ): Uri {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.Audio.Media.TITLE, trackInfo.title)
            put(MediaStore.Audio.Media.ARTIST, trackInfo.uploader)
            put(MediaStore.Audio.Media.MIME_TYPE, format.mimeType)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$SUB_DIRECTORY")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: error("Failed to create MediaStore entry")

        resolver.openOutputStream(uri)?.use { output ->
            FileInputStream(sourceFile).use { input ->
                input.copyTo(output)
            }
        } ?: error("Failed to open output stream for MediaStore")

        contentValues.clear()
        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)

        sourceFile.delete()
        return uri
    }

    private fun saveViaLegacyStorage(
        sourceFile: File,
        trackInfo: TrackInfo,
        format: AudioFormat
    ): Uri {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val targetDir = File(musicDir, SUB_DIRECTORY).apply { if (!exists()) mkdirs() }
        val destinationFile = File(targetDir, sourceFile.name)

        FileInputStream(sourceFile).use { input ->
            destinationFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        sourceFile.delete()

        MediaScannerConnection.scanFile(
            context,
            arrayOf(destinationFile.absolutePath),
            arrayOf(format.mimeType),
            null
        )

        return Uri.fromFile(destinationFile)
    }

    companion object {
        private const val SUB_DIRECTORY = "YTDownloader"
    }
}

