package com.personal.ytmusicdownloader

import android.app.Application
import android.util.Log
import com.personal.ytmusicdownloader.di.AppContainer
import com.personal.ytmusicdownloader.di.DefaultAppContainer
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class YTDownloaderApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        initYoutubeDl()
    }

    private fun initYoutubeDl() {
        runCatching {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
        }.onFailure { throwable ->
            Log.e("YTDownloaderApp", "Initialization failed", throwable)
        }
    }
}
