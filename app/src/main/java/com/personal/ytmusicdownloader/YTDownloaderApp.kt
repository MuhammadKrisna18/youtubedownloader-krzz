package com.personal.ytmusicdownloader

import android.app.Application
import android.util.Log
import com.personal.ytmusicdownloader.di.AppContainer
import com.personal.ytmusicdownloader.di.DefaultAppContainer
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class YTDownloaderApp : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        initYoutubeDl()
    }

    private fun initYoutubeDl() {
        runCatching {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
        }.onSuccess {
            applicationScope.launch {
                runCatching {
                    YoutubeDL.getInstance().updateYoutubeDL(this@YTDownloaderApp, YoutubeDL.UpdateChannel.STABLE)
                }
            }
        }.onFailure { throwable ->
            Log.e("YTDownloaderApp", "Initialization failed", throwable)
        }
    }
}
