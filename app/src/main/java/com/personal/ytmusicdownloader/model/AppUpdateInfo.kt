package com.personal.ytmusicdownloader.model

data class AppUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSize: Long,
    val isUpdateAvailable: Boolean
)

