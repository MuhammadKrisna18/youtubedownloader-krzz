package com.personal.ytmusicdownloader.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.ytmusicdownloader.data.AppUpdateManager
import com.personal.ytmusicdownloader.data.CookieStorage
import com.personal.ytmusicdownloader.data.DownloadRepository
import com.personal.ytmusicdownloader.model.AudioFormat
import com.personal.ytmusicdownloader.model.DownloadProgress
import com.personal.ytmusicdownloader.service.DownloadService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val downloadRepository: DownloadRepository,
    private val cookieStorage: CookieStorage,
    private val appUpdateManager: AppUpdateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refreshCookieInfo()
        observeDownloadProgress()
    }

    private fun observeDownloadProgress() {
        viewModelScope.launch {
            DownloadService.downloadState.collect { progress ->
                _uiState.update { current ->
                    current.copy(downloadProgress = progress)
                }
            }
        }
    }

    fun onUrlChanged(url: String) {
        _uiState.update { it.copy(url = url.trim()) }
    }

    fun fetchTrackInfo() {
        val currentUrl = _uiState.value.url
        if (currentUrl.isBlank()) {
            _uiState.update { it.copy(userMessage = "Masukkan URL YouTube terlebih dahulu") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInfo = true, userMessage = null) }
            downloadRepository.fetchTrackInfo(currentUrl).fold(
                onSuccess = { track ->
                    _uiState.update {
                        it.copy(
                            isLoadingInfo = false,
                            trackInfo = track
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingInfo = false,
                            userMessage = error.localizedMessage ?: "Gagal mengambil data video"
                        )
                    }
                }
            )
        }
    }

    fun selectFormat(format: AudioFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun startDownload(context: Context) {
        val state = _uiState.value
        val track = state.trackInfo ?: return
        if (state.downloadProgress is DownloadProgress.Downloading || state.downloadProgress is DownloadProgress.Transcoding) {
            return
        }

        DownloadService.start(
            context = context,
            url = track.webUrl,
            trackInfo = track,
            format = state.selectedFormat
        )
    }

    fun cancelDownload(context: Context) {
        DownloadService.cancel(context)
    }

    fun importCookie(uri: Uri) {
        cookieStorage.saveCookieFromUri(uri).fold(
            onSuccess = {
                refreshCookieInfo()
                _uiState.update {
                    it.copy(
                        userMessage = "Cookie YouTube berhasil diimpor dengan aman",
                        showCookieDialog = false
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(userMessage = "Gagal mengimpor cookies: ${error.localizedMessage}")
                }
            }
        )
    }

    fun saveCookieText(text: String) {
        cookieStorage.saveCookieText(text).fold(
            onSuccess = {
                refreshCookieInfo()
                _uiState.update {
                    it.copy(
                        userMessage = "Cookie YouTube berhasil disimpan dengan aman",
                        showCookieDialog = false
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(userMessage = "Gagal menyimpan cookies: ${error.localizedMessage}")
                }
            }
        )
    }

    fun restoreDefaultCookie() {
        cookieStorage.restoreDefaultCookie().fold(
            onSuccess = {
                refreshCookieInfo()
                _uiState.update {
                    it.copy(
                        userMessage = "Cookie bawaan berhasil dipasang",
                        showCookieDialog = false
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(userMessage = "Gagal memasang cookie bawaan: ${error.localizedMessage}")
                }
            }
        )
    }

    fun deleteCookie() {
        val deleted = cookieStorage.deleteCookie()
        refreshCookieInfo()
        _uiState.update {
            it.copy(
                userMessage = if (deleted) "cookies.txt berhasil dihapus" else "File cookie tidak ditemukan",
                showCookieDialog = false
            )
        }
    }

    private fun refreshCookieInfo() {
        val info = cookieStorage.getCookieInfo()
        _uiState.update { it.copy(cookieInfo = info) }
    }

    fun checkForUpdate(currentVersion: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true) }
            appUpdateManager.checkForUpdate(currentVersion).fold(
                onSuccess = { update ->
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateInfo = update,
                            showUpdateDialog = update.isUpdateAvailable,
                            userMessage = if (!update.isUpdateAvailable) "Aplikasi sudah dalam versi terbaru" else null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            userMessage = "Gagal memeriksa pembaruan: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun downloadAndInstallUpdate() {
        val downloadUrl = _uiState.value.updateInfo?.downloadUrl.orEmpty()
        if (downloadUrl.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingUpdate = true, updateProgress = 0f) }
            appUpdateManager.downloadUpdate(downloadUrl) { progress, _, _ ->
                _uiState.update { it.copy(updateProgress = progress) }
            }.fold(
                onSuccess = { apkFile ->
                    _uiState.update {
                        it.copy(
                            isDownloadingUpdate = false,
                            showUpdateDialog = false
                        )
                    }
                    appUpdateManager.installApk(apkFile).onFailure { error ->
                        _uiState.update {
                            it.copy(userMessage = "Gagal memulai instalasi: ${error.localizedMessage}")
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDownloadingUpdate = false,
                            userMessage = "Gagal mengunduh berkas pembaruan: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun openCookieDialog() {
        _uiState.update { it.copy(showCookieDialog = true) }
    }

    fun dismissCookieDialog() {
        _uiState.update { it.copy(showCookieDialog = false) }
    }

    fun openUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = true) }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}

class MainViewModelFactory(
    private val downloadRepository: DownloadRepository,
    private val cookieStorage: CookieStorage,
    private val appUpdateManager: AppUpdateManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(downloadRepository, cookieStorage, appUpdateManager) as T
    }
}

