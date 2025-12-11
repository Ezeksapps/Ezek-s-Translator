package com.ezeksapps.ezeksapp.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezeksapps.ezeksapp.data.DataStoreManager
import com.ezeksapps.ezeksapp.model.DownloadableModel
import com.ezeksapps.ezeksapp.network.DownloadStatus
import com.ezeksapps.ezeksapp.network.ModelService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Model type constants for consistent usage throughout ViewModel
private const val MODEL_TYPE_LITE = "lite"
private const val MODEL_TYPE_FULL = "full"

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val modelService: ModelService
) : ViewModel() {

    private val _isSetupComplete = MutableStateFlow(false)
    val isSetupComplete: StateFlow<Boolean> = _isSetupComplete.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _availableDownloadableModels = MutableStateFlow<List<DownloadableModel>>(emptyList())
    val availableDownloadableModel: StateFlow<List<DownloadableModel>> = _availableDownloadableModels.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    private val _loadingError = MutableStateFlow<String?>(null)
    val loadingError: StateFlow<String?> = _loadingError.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    // values from 0f to 1f
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _downloadStatus = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStatus: StateFlow<Map<String, DownloadStatus>> = _downloadStatus.asStateFlow()

    private val _selectedModelType = MutableStateFlow(MODEL_TYPE_LITE)
    val selectedModelType: StateFlow<String> = _selectedModelType.asStateFlow()

    private val _liteSelections = MutableStateFlow<Set<String>>(emptySet())
    private val _fullSelections = MutableStateFlow<Set<String>>(emptySet())

    // combines model type and separate selection sets
    // auto-updates UI when either tab selection or model selection changes
    val currentSelections: StateFlow<Set<String>> = combine(
        _selectedModelType,
        _liteSelections,
        _fullSelections
    ) { modelType, liteSelections, fullSelections ->
        when (modelType) {
            MODEL_TYPE_LITE -> liteSelections
            MODEL_TYPE_FULL -> fullSelections
            else -> emptySet()
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), // halt if UI inactive
        initialValue = emptySet()
    )

    init {
        // Check if user has already completed setup
        checkSetupStatus()
    }

    // Check if setup was previously completed
    private fun checkSetupStatus() {
        viewModelScope.launch {
            dataStoreManager.setupCompleted.collect { isComplete ->
                _isSetupComplete.value = isComplete
                _isLoading.value = false
            }
        }
    }

    fun loadAvailableModels() {
        viewModelScope.launch {
            _isLoadingModels.value = true
            _loadingError.value = null
            try {
                val models = modelService.getAvailableModels()
                _availableDownloadableModels.value = models
            } catch (e: Exception) {
                _loadingError.value = e.message ?: "Failed to load available models"
            } finally {
                _isLoadingModels.value = false
            }
        }
    }

    // Retry loading models after an error
    fun retryModelLoading() {
        _availableDownloadableModels.value = emptyList()
        _loadingError.value = null
        loadAvailableModels()
    }

    fun downloadSelectedModels() {
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = emptyMap()
            _downloadStatus.value = emptyMap()

            try {
                val allSelections = _liteSelections.value + _fullSelections.value

                // Handle IDs with model type suffix (e.g., "en-es-lite")
                val modelsToDownload = _availableDownloadableModels.value.filter { model ->
                    val modelKey = "${model.id}-${model.modelType}"
                    allSelections.contains(modelKey)
                }

                if (modelsToDownload.isEmpty()) {
                    // No models to download, just complete setup
                    _isDownloading.value = false
                    return@launch
                }

                handleModelsDownload(modelsToDownload)

            } catch (e: Exception) {
                _loadingError.value = "Download failed: ${e.message}"
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun completeSetup(appLang: String, onlineMode: Boolean) {
        viewModelScope.launch {
            try {
                // save user prefs
                dataStoreManager.setOnlineModeEnabled(onlineMode)
                dataStoreManager.setUserDefaultLang(appLang)
                dataStoreManager.setSetupCompleted(true)
                _isSetupComplete.value = true
            } catch (e: Exception) {
                _loadingError.value = "Setup failed: ${e.message}"
            }
        }
    }

    // Switch between model type tabs in ModelSelectPage
    fun setModelType(modelType: String) {
        // Save current selections before switching
        when (_selectedModelType.value) {
            MODEL_TYPE_LITE -> {
                _liteSelections.value = _liteSelections.value.toSet()
            }
            MODEL_TYPE_FULL -> {
                _fullSelections.value = _fullSelections.value.toSet()
            }
        }
        _selectedModelType.value = modelType
    }

    fun getCurrentSelections(): Set<String> {
        return when (_selectedModelType.value) {
            MODEL_TYPE_LITE -> _liteSelections.value
            MODEL_TYPE_FULL -> _fullSelections.value
            else -> emptySet()
        }
    }

    fun updateCurrentSelections(selections: Set<String>) {
        val newSet = selections.toSet() // creates new instance

        when (_selectedModelType.value) {
            MODEL_TYPE_LITE -> _liteSelections.value = newSet
            MODEL_TYPE_FULL -> _fullSelections.value = newSet
        }
    }

    private suspend fun handleModelsDownload(modelsToDownload: List<DownloadableModel>) {
        // init all models as DOWNLOADING status
        val initialStatus = modelsToDownload.associate { it.id to DownloadStatus.DOWNLOADING }
        _downloadStatus.value = initialStatus

        // async all downloads so the page doesn't escape after only one is done
        coroutineScope {
            modelsToDownload.map { model ->
                async {
                    modelService.downloadModel(model) { downloadProgress ->
                        // Update progress for this model
                        _downloadProgress.value = _downloadProgress.value +
                                (downloadProgress.modelId to downloadProgress.progress)

                        // Update status for this model
                        _downloadStatus.value = _downloadStatus.value +
                                (downloadProgress.modelId to downloadProgress.status)
                    }
                }
            }.awaitAll()
        }
    }
}