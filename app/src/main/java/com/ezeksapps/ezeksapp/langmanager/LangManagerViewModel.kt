package com.ezeksapps.ezeksapp.langmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezeksapps.ezeksapp.core.utils.LangUtils
import com.ezeksapps.ezeksapp.core.utils.ModelUtils
import com.ezeksapps.ezeksapp.model.DownloadableModel
import com.ezeksapps.ezeksapp.model.DownloadableModelPair
import com.ezeksapps.ezeksapp.model.Lang
import com.ezeksapps.ezeksapp.model.TranslationModel
import com.ezeksapps.ezeksapp.model.TranslationModelPair
import com.ezeksapps.ezeksapp.network.DownloadStatus
import com.ezeksapps.ezeksapp.network.ModelService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LangManagerViewModel @Inject constructor(
    private val modelUtils: ModelUtils,
    private val modelService: ModelService
) : ViewModel() {

    private val _installedModels = MutableStateFlow<List<TranslationModel>>(emptyList())
    val installedModels: StateFlow<List<TranslationModel>> = _installedModels.asStateFlow()

    private val _availableModels = MutableStateFlow<List<DownloadableModel>>(emptyList())
    val availableModels: StateFlow<List<DownloadableModel>> = _availableModels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    init {
        loadInstalledModels()
    }

    fun loadInstalledModels() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val installedModelsMap = modelUtils.getInstalledModels()
                val models = mutableListOf<TranslationModel>()

                installedModelsMap.forEach { (modelId, modelTypes) ->
                    val codes = modelId.split("-")
                    if (codes.size == 2) {
                        val sourceLang = Lang(codes[0], LangUtils.getLangName(codes[0]))
                        val targetLang = Lang(codes[1], LangUtils.getLangName(codes[1]))

                        modelTypes.forEach { modelType ->
                            models.add(
                                TranslationModel(
                                    id = modelId,
                                    sourceLang = sourceLang,
                                    targetLang = targetLang,
                                    displayName = "${sourceLang.name} to ${targetLang.name}",
                                    modelType = modelType,
                                    isDownloaded = true
                                )
                            )
                        }
                    }
                }

                _installedModels.value = models
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load installed models: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAvailableModels() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val allDownloadableModels = modelService.getAvailableModels()

                // Create a set of installed model keys (including model type) - matches SetupScreen format
                val installedModelKeys = _installedModels.value.map {
                    "${it.id}-${it.modelType}"
                }.toSet()

                // Filter out models that are already installed (check both ID and type)
                _availableModels.value = allDownloadableModels.filter { model ->
                    "${model.id}-${model.modelType}" !in installedModelKeys
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load available models: ${e.message}"
                _availableModels.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun installModelPair(pair: DownloadableModelPair) {
        viewModelScope.launch {
            val forwardJob = async { installModel(pair.forwardModel) }
            val reverseJob = async { installModel(pair.reverseModel) }
            forwardJob.await()
            reverseJob.await()
        }
    }

    fun installModel(model: DownloadableModel) {
        viewModelScope.launch {
            // Use the same ID format as SetupScreen: "${model.id}-${model.modelType}"
            val modelKey = "${model.id}-${model.modelType}"

            // Check if already downloading to prevent duplicate downloads
            if (_downloadStates.value[modelKey]?.status == DownloadStatus.DOWNLOADING ||
                _downloadStates.value[modelKey]?.status == DownloadStatus.EXTRACTING) {
                return@launch
            }

            _downloadStates.value = _downloadStates.value + (modelKey to DownloadState(DownloadStatus.DOWNLOADING, 0f))

            try {
                modelService.downloadModel(model) { progress ->
                    _downloadStates.value = _downloadStates.value + (modelKey to DownloadState(progress.status, progress.progress))

                    when (progress.status) {
                        DownloadStatus.COMPLETED, DownloadStatus.FAILED -> {
                            // Use async to ensure both loads complete before clearing state
                            viewModelScope.launch {
                                loadInstalledModels()
                                loadAvailableModels()
                                _downloadStates.value = _downloadStates.value - modelKey

                                if (progress.status == DownloadStatus.FAILED) {
                                    _errorMessage.value = "Failed to download model: ${model.id} (${model.modelType})"
                                }
                            }
                        }
                        else -> {
                            // DOWNLOADING/EXTRACTING handled in composable
                        }
                    }
                }
            } catch (e: Exception) {
                _downloadStates.value = _downloadStates.value - modelKey
                _errorMessage.value = "Failed to download model: ${e.message}"
            }
        }
    }

    fun removeModelPair(pair: TranslationModelPair) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                removeModel(pair.forwardModel)
                removeModel(pair.reverseModel)
                loadInstalledModels()
                loadAvailableModels()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to remove model pair: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeModel(model: TranslationModel) {
        viewModelScope.launch {
            try {
                val modelDir = modelUtils.getModelDir(model.id, model.modelType)
                if (modelDir.exists()) {
                    modelDir.deleteRecursively()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to remove model: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

data class DownloadState(
    val status: DownloadStatus,
    val progress: Float
)