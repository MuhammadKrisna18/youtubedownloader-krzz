package com.personal.ytmusicdownloader.data

import android.content.Context
import android.net.Uri
import com.personal.ytmusicdownloader.model.CookieInfo
import java.io.File

class CookieStorage(private val context: Context) {

    private val targetCookieFile: File
        get() = File(context.filesDir, COOKIE_FILE_NAME)

    init {
        ensureCookieInstalled()
    }

    fun ensureCookieInstalled() {
        if (!hasCookie()) {
            restoreDefaultCookie()
        }
    }

    fun restoreDefaultCookie(): Result<Unit> = runCatching {
        val defaultText = DefaultCookieProvider.getDecryptedCookie()
            ?: error("Cookie bawaan tidak dapat dimuat")
        saveCookieText(defaultText).getOrThrow()
    }

    fun saveCookieFromUri(uri: Uri): Result<Unit> = runCatching {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Gagal membaca file dari URI")
        saveCookieText(content).getOrThrow()
    }

    fun saveCookieText(rawContent: String): Result<Unit> = runCatching {
        val sanitized = sanitize(rawContent)
        val count = countEntries(sanitized)
        if (count == 0) {
            error("Tidak ada entri cookie YouTube atau Google yang valid ditemukan")
        }
        targetCookieFile.writeText(sanitized)
    }

    fun getCookieFile(): File? = if (hasCookie()) targetCookieFile else null

    fun hasCookie(): Boolean = targetCookieFile.exists() && targetCookieFile.length() > 0

    fun getCookieInfo(): CookieInfo {
        val exists = hasCookie()
        val count = if (exists) countEntries(targetCookieFile.readText()) else 0
        return CookieInfo(
            exists = exists,
            sizeBytes = if (exists) targetCookieFile.length() else 0L,
            lastModified = if (exists) targetCookieFile.lastModified() else 0L,
            entryCount = count
        )
    }

    fun deleteCookie(): Boolean = if (targetCookieFile.exists()) targetCookieFile.delete() else false

    private fun sanitize(raw: String): String {
        val builder = StringBuilder()
        builder.append(COOKIE_HEADER).append("\n\n")
        raw.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val parts = line.split("\t")
                if (parts.size >= 7) {
                    val domain = parts[0].trim().lowercase()
                    if (domain.endsWith("youtube.com") || domain == ".google.com" || domain == "google.com") {
                        builder.append(line.trimEnd()).append("\n")
                    }
                }
            }
        }
        return builder.toString()
    }

    private fun countEntries(content: String): Int {
        return content.lineSequence().count { line ->
            val trimmed = line.trim()
            trimmed.isNotEmpty() && !trimmed.startsWith("#") && line.split("\t").size >= 7
        }
    }

    companion object {
        private const val COOKIE_FILE_NAME = "cookies.txt"
        private const val COOKIE_HEADER = "# Netscape HTTP Cookie File\n# https://curl.haxx.se/rfc/cookie_spec.html\n# This is a generated file! Do not edit."
    }
}
