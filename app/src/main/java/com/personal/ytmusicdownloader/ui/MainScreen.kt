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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.ytmusicdownloader.BuildConfig
import com.personal.ytmusicdownloader.model.DownloadProgress
import com.personal.ytmusicdownloader.ui.components.ActiveDownloadCard
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
        viewModel.checkForUpdate(BuildConfig.VERSION_NAME, isAutoCheck = true)
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
                isCheckingUpdate = uiState.isCheckingUpdate,
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
                onAnalyzeClick = { viewModel.fetchTrackInfo() },
                onPasteAndAnalyze = { viewModel.fetchTrackInfo(it) }
            )

            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(animationSpec = tween(350)) + expandVertically(animationSpec = tween(350)),
                exit = fadeOut(animationSpec = tween(250)) + shrinkVertically(animationSpec = tween(250))
            ) {
                uiState.errorMessage?.let { errorText ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

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
                        isDownloading = uiState.downloadProgress is DownloadProgress.Downloading ||
                                uiState.downloadProgress is DownloadProgress.Transcoding,
                        onDownloadClick = { viewModel.startDownload(context) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
