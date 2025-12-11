package com.ezeksapps.ezeksapp.translator

/* NOTE: View Models separate the internal operations of a Screen or View (whatever they're technically called)
* from the main Composable UI code & other front-end operations. For some reason, the Android Docs like to call
* back-end logic 'business logic'. I am not going to be doing that, because that makes me sounds like a faceless
* corporate entity who couldn't care less about the user, & I am not writing code comments that make me sound like Micro$oft */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezeksapps.ezeksapp.core.utils.LangUtils
import com.ezeksapps.ezeksapp.core.utils.ModelUtils
import com.ezeksapps.ezeksapp.data.DataStoreManager
import com.ezeksapps.ezeksapp.model.Lang
import com.ezeksapps.ezeksapp.model.LangSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranslatorScreenViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val translationManager: TranslationManager,
    private val modelUtils: ModelUtils // yet another wierd strategy that only exists because of context being a pain
) : ViewModel() {

    /* From what I've seen the convention is to prefix private values with _
    * I believe there could be a better way of representing that but this is fine for now */

    // uses LangSelection as Lang cannot handle modelType
    private val _langs = MutableStateFlow<List<LangSelection>>(emptyList())
    val langs: StateFlow<List<LangSelection>> = _langs.asStateFlow()

    private val _sourceLang = MutableStateFlow<LangSelection?>(null)
    val sourceLang: StateFlow<LangSelection?> = _sourceLang.asStateFlow()

    private val _targetLang = MutableStateFlow<LangSelection?>(null)
    val targetLang: StateFlow<LangSelection?> = _targetLang.asStateFlow()

    /* Used for the loading icons when performing translation, however, it could be good to make the translation constantly update
    * as the user types like in other translator apps, maybe implement this in a future build? */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _translationResult = MutableStateFlow("")
    val translationResult: StateFlow<String> = _translationResult.asStateFlow()

    private val _isOnlineMode = MutableStateFlow(false)
    val isOnlineMode: StateFlow<Boolean> = _isOnlineMode.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _modelType = MutableStateFlow<String?>(null)
    val modelType: StateFlow<String?> = _modelType.asStateFlow()

    init {
        // Load initial states sequentially to avoid race conditions
        viewModelScope.launch {
            // Get initial values first
            val isOnline = dataStoreManager.onlineModeEnabled.first()
            val userLang = dataStoreManager.userDefaultLang.first()

            _isOnlineMode.value = isOnline
            // Load languages with the initial values
            loadAvailableLangs(isOnline, userLang)

            // Start observing changes after initial load
            observeOnlineMode()
        }
    }

    private fun observeOnlineMode() {
        viewModelScope.launch {
            // Skip the first emission (already handled in init)
            dataStoreManager.onlineModeEnabled.collect { isOnline ->
                // Only reload if online mode actually changed
                if (_isOnlineMode.value != isOnline) {
                    _isOnlineMode.value = isOnline
                    loadAvailableLangs(isOnline, dataStoreManager.userDefaultLang.first())
                }
            }
        }
    }

    private fun loadAvailableLangs(isOnline: Boolean, userLang: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val loadedLangs = if (isOnline) {
                    // Online mode, no model type distinction
                    val onlineLangs = translationManager.getLangs()
                    listOf(LangSelection(Lang("auto", "Detect Language"))) +
                            onlineLangs.map { LangSelection(it) }
                } else {
                    // lookup what models are locally installed
                    val installedModels = modelUtils.getInstalledModels()
                    val selections = mutableListOf<LangSelection>()

                    // add Detect Language to options as well
                    selections.add(LangSelection(Lang("auto", "Detect Language")))

                    // adds found local NLP models along with their type (full or lite)
                    installedModels.forEach { (modelId, modelTypes) ->
                        val codes = modelId.split("-")
                        if (codes.size == 2) { // just in case users like changing dir names for no reason
                            val sourceLang = Lang(codes[0], LangUtils.getLangName(codes[0]))
                            val targetLang = Lang(codes[1], LangUtils.getLangName(codes[1]))

                            modelTypes.forEach { modelType ->
                                selections.add(LangSelection(sourceLang, modelType))
                            }
                            modelTypes.forEach { modelType ->
                                selections.add(LangSelection(targetLang, modelType))
                            }
                        }
                    }

                    // remove dupes & sort
                    selections.distinctBy { it.code to it.modelType }
                        .sortedBy { it.displayName }
                }

                // Update the langs list FIRST
                _langs.value = loadedLangs

                // Then set source and target languages based on the updated list
                // sourceLang will always be auto-set to Detect Language, targetLang gets set to USER_DEFAULT_LANG
                _sourceLang.value = loadedLangs.firstOrNull { it.code == "auto" }
                _targetLang.value = loadedLangs.firstOrNull { it.code == userLang }

                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load languages: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun translateText(text: String) {
        viewModelScope.launch {
            if (text.isBlank()) {
                _translationResult.value = ""
                return@launch
            }

            // assuming I've done everything correctly the values should never end up null, but Kotlin seems to enforce null handling
            val sourceLang = _sourceLang.value ?: run {
                _errorMessage.value = "Please select a language to translate from"
                return@launch
            }
            val targetLang = _targetLang.value ?: run {
                _errorMessage.value = "Please select a language to translate to"
                return@launch
            }

            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = if (_isOnlineMode.value) {
                    // Online translation
                    translationManager.translate(
                        text,
                        sourceLang.code,
                        targetLang.code
                    )
                } else {
                    // Offline translation
                    translationManager.translate(
                        text,
                        sourceLang.code,
                        targetLang.code,
                        targetLang.modelType ?: "lite" // this should never fail, if it does, something is horribly wrong with the code
                    )
                }
                _translationResult.value = result
            } catch (e: Exception) {
                _translationResult.value = ""
                _errorMessage.value = "Translation failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSourceLang(selection: LangSelection) {
        _sourceLang.value = selection
        _modelType.value = selection.modelType
    }

    fun setTargetLang(selection: LangSelection) {
        _targetLang.value = selection
        _modelType.value = selection.modelType ?: _modelType.value
    }

    /* This has nothing to do with the ViewModel, It's just here so I don't forget to put in in the future SettingViewModel
    fun setUserDefaultLang(langCode: String) {
        viewModelScope.launch {
            dataStoreManager.setUserDefaultLang(langCode)
        }
    }*/

    fun setOnlineMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setOnlineModeEnabled(enabled)
        }
    }

    override fun onCleared() {
        translationManager.shutdown()
        super.onCleared()
    }
}