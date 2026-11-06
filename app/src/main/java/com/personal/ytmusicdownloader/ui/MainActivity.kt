package com.personal.ytmusicdownloader.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.personal.ytmusicdownloader.YTDownloaderApp
import com.personal.ytmusicdownloader.ui.theme.YTDownloaderTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as YTDownloaderApp
        MainViewModelFactory(
            downloadRepository = app.container.downloadRepository,
            appUpdateManager = app.container.appUpdateManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent {
            YTDownloaderTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            val url = extractUrl(sharedText)
            if (url.isNotBlank()) {
                viewModel.fetchTrackInfo(url)
            }
        }
    }

    private fun extractUrl(text: String): String {
        val urlRegex = """https?://[^\s]+""".toRegex()
        return urlRegex.find(text)?.value.orEmpty()
    }
}
