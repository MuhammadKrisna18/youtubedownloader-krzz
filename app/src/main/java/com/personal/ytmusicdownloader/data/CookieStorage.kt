package com.personal.ytmusicdownloader.data

import android.content.Context
import android.net.Uri
import com.personal.ytmusicdownloader.model.CookieInfo
import java.io.File

class CookieStorage(private val context: Context) {

    private val targetCookieFile: File
        get() = File(context.filesDir, COOKIE_FILE_NAME)

    fun saveCookieFromUri(uri: Uri): Result<Unit> = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            targetCookieFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("Failed to open stream for URI")
    }

    fun getCookieFile(): File? = if (hasCookie()) targetCookieFile else null

    fun hasCookie(): Boolean = targetCookieFile.exists() && targetCookieFile.length() > 0

    fun getCookieInfo(): CookieInfo {
        val exists = hasCookie()
        return CookieInfo(
            exists = exists,
            sizeBytes = if (exists) targetCookieFile.length() else 0L,
            lastModified = if (exists) targetCookieFile.lastModified() else 0L
        )
    }

    fun deleteCookie(): Boolean = if (targetCookieFile.exists()) targetCookieFile.delete() else false

    companion object {
        private const val COOKIE_FILE_NAME = "cookies.txt"
    }
}
