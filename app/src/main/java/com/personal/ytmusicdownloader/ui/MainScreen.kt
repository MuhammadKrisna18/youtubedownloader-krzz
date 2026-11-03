package com.personal.ytmusicdownloader.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.ytmusicdownloader.BuildConfig
import com.personal.ytmusicdownloader.model.DownloadProgress
import com.personal.ytmusicdownloader.ui.components.ActiveDownloadCard
import com.personal.ytmusicdownloader.ui.components.CookieDialog
import com.personal.ytmusicdownloader.ui.components.TopBar
import com.personal.ytmusicdownloader.ui.components.TrackPreviewCard
import com.personal.ytmusicdownloader.ui.components.UpdateDialog
import com.personal.ytmusicdownloader.ui.components.UrlInputSection

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        viewModel.checkForUpdate(BuildConfig.VERSION_NAME)
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            TopBar(
                hasCookie = uiState.cookieInfo.exists,
                isCheckingUpdate = uiState.isCheckingUpdate,
                onCookieClick = { viewModel.openCookieDialog() },
                onUpdateClick = { viewModel.checkForUpdate(BuildConfig.VERSION_NAME) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            UrlInputSection(
                url = uiState.url,
                isLoading = uiState.isLoadingInfo,
                onUrlChange = { viewModel.onUrlChanged(it) },
                onAnalyzeClick = { viewModel.fetchTrackInfo() }
            )

            ActiveDownloadCard(
                progress = uiState.downloadProgress,
                onCancelClick = { viewModel.cancelDownload(context) }
            )

            AnimatedVisibility(
                visible = uiState.trackInfo != null,
                enter = fadeIn(animationSpec = tween(350)) + expandVertically(animationSpec = tween(350)),
                exit = fadeOut(animationSpec = tween(250)) + shrinkVertically(animationSpec = tween(250))
            ) {
                uiState.trackInfo?.let { track ->
                    TrackPreviewCard(
                        trackInfo = track,
                        selectedFormat = uiState.selectedFormat,
                        isDownloading = uiState.downloadProgress is DownloadProgress.Downloading ||
                                uiState.downloadProgress is DownloadProgress.Transcoding,
                        onFormatSelect = { viewModel.selectFormat(it) },
                        onDownloadClick = { viewModel.startDownload(context) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (uiState.showCookieDialog) {
            CookieDialog(
                cookieInfo = uiState.cookieInfo,
                onImportCookie = { uri -> viewModel.importCookie(uri) },
                onSaveCookieText = { text -> viewModel.saveCookieText(text) },
                onRestoreDefaultCookie = { viewModel.restoreDefaultCookie() },
                onDeleteCookie = { viewModel.deleteCookie() },
                onDismiss = { viewModel.dismissCookieDialog() }
            )
        }

        if (uiState.showUpdateDialog && uiState.updateInfo != null) {
            UpdateDialog(
                updateInfo = uiState.updateInfo!!,
                isDownloading = uiState.isDownloadingUpdate,
                progress = uiState.updateProgress,
                onInstallClick = { viewModel.downloadAndInstallUpdate() },
                onDismiss = { viewModel.dismissUpdateDialog() }
            )
        }
    }
}
