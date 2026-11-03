package com.personal.ytmusicdownloader.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.personal.ytmusicdownloader.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class AppUpdateManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val owner: String = "MuhammadKrisna18",
    private val repo: String = "youtubedownloader-krzz"
) {

    suspend fun checkForUpdate(currentVersion: String): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "YTMusicDownloader-Android")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.code == 404) {
                return@runCatching AppUpdateInfo(
                    currentVersion = currentVersion,
                    latestVersion = currentVersion,
                    releaseTitle = "",
                    releaseNotes = "",
                    downloadUrl = "",
                    apkSize = 0L,
                    isUpdateAvailable = false
                )
            }

            if (!response.isSuccessful) {
                error("HTTP error: ${response.code}")
            }

            val bodyString = response.body?.string().orEmpty()
            val json = JSONObject(bodyString)
            val tagName = json.optString("tag_name", "")
            val title = json.optString("name", tagName)
            val body = json.optString("body", "")

            var downloadUrl = ""
            var apkSize = 0L

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val updateAvailable = tagName.isNotBlank() &&
                    downloadUrl.isNotBlank() &&
                    isNewerVersion(currentVersion, tagName)

            AppUpdateInfo(
                currentVersion = currentVersion,
                latestVersion = tagName,
                releaseTitle = title,
                releaseNotes = body,
                downloadUrl = downloadUrl,
                apkSize = apkSize,
                isUpdateAvailable = updateAvailable
            )
        }
    }

    suspend fun downloadUpdate(
        downloadUrl: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "YTMusicDownloader-Android")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                error("Failed to download APK: ${response.code}")
            }

            val body = response.body ?: error("Empty response body")
            val totalBytes = body.contentLength()

            val updateDir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
            val apkFile = File(updateDir, "update.apk").apply { if (exists()) delete() }

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f
                        onProgress(progress, totalRead, totalBytes)
                    }
                    output.flush()
                }
            }

            apkFile
        }
    }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun installApk(apkFile: File): Result<Unit> = runCatching {
        if (!apkFile.exists()) {
            error("APK file does not exist")
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    companion object {
        fun isNewerVersion(current: String, latest: String): Boolean {
            val cleanCurrent = current.trimStart('v', 'V').split("-")[0].trim()
            val cleanLatest = latest.trimStart('v', 'V').split("-")[0].trim()

            val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
            val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }

            val maxLength = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until maxLength) {
                val curr = currentParts.getOrElse(i) { 0 }
                val lat = latestParts.getOrElse(i) { 0 }
                if (lat > curr) return true
                if (lat < curr) return false
            }
            return false
        }
    }
}

