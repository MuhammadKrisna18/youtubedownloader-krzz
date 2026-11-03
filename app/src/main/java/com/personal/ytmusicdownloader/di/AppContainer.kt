package com.personal.ytmusicdownloader.di

import android.content.Context
import com.personal.ytmusicdownloader.data.AppUpdateManager
import com.personal.ytmusicdownloader.data.CookieStorage
import com.personal.ytmusicdownloader.data.DownloadRepository
import com.personal.ytmusicdownloader.data.MediaStorageManager
import com.personal.ytmusicdownloader.data.YoutubeDlRepository

interface AppContainer {
    val cookieStorage: CookieStorage
    val downloadRepository: DownloadRepository
    val mediaStorageManager: MediaStorageManager
    val appUpdateManager: AppUpdateManager
}

class DefaultAppContainer(context: Context) : AppContainer {

    override val cookieStorage: CookieStorage by lazy {
        CookieStorage(context)
    }

    override val mediaStorageManager: MediaStorageManager by lazy {
        MediaStorageManager(context)
    }

    override val downloadRepository: DownloadRepository by lazy {
        YoutubeDlRepository(context, cookieStorage)
    }

    override val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManager(context)
    }
}
