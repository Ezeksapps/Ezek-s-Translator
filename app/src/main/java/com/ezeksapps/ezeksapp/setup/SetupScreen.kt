package com.ezeksapps.ezeksapp.setup


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ezeksapps.ezeksapp.R
import com.ezeksapps.ezeksapp.model.DownloadableModel
import com.ezeksapps.ezeksapp.model.DownloadableModelPair
import com.ezeksapps.ezeksapp.model.Lang
import com.ezeksapps.ezeksapp.network.DownloadStatus


/* I think this is the longest file in the app currently? potential todo: add more comments & tidy code */

/* PAGE IDs  */
private const val LANG_SELECT_PAGE = 0
private const val MODELS_SELECT_PAGE = 1
private const val DOWNLOAD_PAGE = 2

/* ModelType consts */
private const val MODEL_TYPE_LITE = "lite"
private const val MODEL_TYPE_FULL = "full"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
) {
    val viewModel: SetupViewModel = viewModel()
    val availableDownloadableModels by viewModel.availableDownloadableModel.collectAsState(emptyList())
    val isLoadingModels by viewModel.isLoadingModels.collectAsState()
    val loadingError by viewModel.loadingError.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()
    val selectedModelType by viewModel.selectedModelType.collectAsState()

    // UI state is handled by SetupScreenState, which contains some commonly-used vals
    var uiState by remember { mutableStateOf(SetupScreenState()) }

    val systemLangCode = LocalConfiguration.current.locales.get(0)?.language ?: "en"
    val currentSelections by viewModel.currentSelections.collectAsState()

    val translationPairs = remember(availableDownloadableModels) {
        generateTranslationPairs(availableDownloadableModels)
    }

    // Handle page navigation, updates state when user switching currentPage
    LaunchedEffect(uiState.currentPage, isDownloading) {
        when {
            uiState.currentPage == MODELS_SELECT_PAGE &&
                    availableDownloadableModels.isEmpty() &&
                    !isLoadingModels -> {
                viewModel.loadAvailableModels()
            }

            uiState.currentPage == DOWNLOAD_PAGE &&
                    !uiState.hasDownloadTriggered -> {
                uiState = uiState.copy(hasDownloadTriggered = true)
                viewModel.downloadSelectedModels()
            }

            uiState.currentPage == DOWNLOAD_PAGE && !isDownloading -> {
                // finish setup after selected models are downloaded (or if user doesn't choose any)
                viewModel.completeSetup(uiState.selectedLang, uiState.enableOnlineMode)
                onSetupComplete()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(getTitleForPage(uiState.currentPage)) },
                navigationIcon = {
                    if (uiState.currentPage > LANG_SELECT_PAGE) {
                        IconButton(onClick = {
                            uiState = uiState.copy(currentPage = uiState.currentPage - 1)
                        }) {
                            Icon(painterResource(R.drawable.arrow_back), contentDescription = "")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Progress indicator TODO: improve visual appearance to match purpose
            LinearProgressIndicator(
                progress = { (uiState.currentPage + 1) / 3f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // run matching composable depending on curPage's value
            when (uiState.currentPage) {
                LANG_SELECT_PAGE -> LangSelectionScreen(
                    selectedLang = uiState.selectedLang,
                    systemLangCode = systemLangCode,
                    onLangSelected = { uiState = uiState.copy(selectedLang = it) },
                    onNext = { uiState = uiState.copy(currentPage = MODELS_SELECT_PAGE) }
                )
                MODELS_SELECT_PAGE -> ModelSelectionScreen(
                    enableOnlineMode = uiState.enableOnlineMode,
                    onOnlineModeChanged = { uiState = uiState.copy(enableOnlineMode = it) },
                    translationPairs = translationPairs,
                    selectedPairs = currentSelections,
                    onPairsChanged = { newSelections ->
                        viewModel.updateCurrentSelections(newSelections)
                    },
                    isLoading = isLoadingModels,
                    loadingError = loadingError,
                    onRetry = viewModel::retryModelLoading,
                    selectedModelType = selectedModelType,
                    onModelTypeChanged = {
                        viewModel.setModelType(it)
                    },
                    onBack = { uiState = uiState.copy(currentPage = LANG_SELECT_PAGE) },
                    onNext = { uiState = uiState.copy(currentPage = DOWNLOAD_PAGE) }
                )
                DOWNLOAD_PAGE -> DownloadScreen(
                    downloadProgress = downloadProgress,
                    downloadStatus = downloadStatus,
                    // Count total unique model & divide by 2 since each pair has 2 models
                    totalModels = currentSelections.size / 2,
                    onFinish = {
                        viewModel.completeSetup(uiState.selectedLang, uiState.enableOnlineMode)
                        onSetupComplete()
                    },
                )
            }
        }
    }
}

private fun getTitleForPage(page: Int): String = when (page) {
    LANG_SELECT_PAGE -> "Select App Language"
    MODELS_SELECT_PAGE -> "Translation Settings"
    DOWNLOAD_PAGE -> "Downloading Models"
    else -> ""
}

/* -- LANG_SELECT_PAGE (still called screen by composable because everything in the app is already a screen) -- */

@Composable
fun LangSelectionScreen(
    selectedLang: String,
    systemLangCode: String,
    onLangSelected: (String) -> Unit,
    onNext: () -> Unit
) {
    val availableLangs = listOf(
        Lang("en", "English"),
        // TODO: Support Spanish, after that? Hope someone takes up other language translations
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Select app language:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedLang == systemLangCode,
                        onClick = { onLangSelected(systemLangCode) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("System Default", style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(availableLangs.filter { it.code != systemLangCode }) { lang ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedLang == lang.code,
                        onClick = { onLangSelected(lang.code) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(lang.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onNext) { Text("Next") }
        }
    }
}

private fun isPairSelected(pair: DownloadableModelPair, selectedPairs: Set<String>): Boolean {
    val forwardId = "${pair.forwardModel.id}-${pair.forwardModel.modelType}"
    val reverseId = "${pair.reverseModel.id}-${pair.reverseModel.modelType}"
    return selectedPairs.contains(forwardId) && selectedPairs.contains(reverseId)
}

/* -- MODELS_SELECT_PAGE -- */
@Composable
fun ModelSelectionScreen(
    enableOnlineMode: Boolean,
    onOnlineModeChanged: (Boolean) -> Unit,
    translationPairs: List<DownloadableModelPair>,
    selectedPairs: Set<String>,
    onPairsChanged: (Set<String>) -> Unit,
    isLoading: Boolean,
    loadingError: String?,
    onRetry: () -> Unit,
    selectedModelType: String,
    onModelTypeChanged: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    /* Note to self: remember() caches the result of a calculation & only re-runs the code block if any of the 'keys'
   * change their value, without remember() the calculation would run every time the composable func is re-drawn
   * which would cause lag & is not a very good idea */

    // Filter pairs by selected model type
    val filteredPairs = remember(translationPairs, selectedModelType) {
        translationPairs.filter { pair ->
            pair.forwardModel.modelType == selectedModelType &&
                    pair.reverseModel.modelType == selectedModelType
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> LoadingState()
            loadingError != null -> ErrorState(loadingError, onRetry)
            else -> {
                // Model Type Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabItem(
                        text = "Lite",
                        isSelected = selectedModelType == MODEL_TYPE_LITE,
                        onClick = {
                            // Just switch tab, selections are stored separately
                            onModelTypeChanged(MODEL_TYPE_LITE)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TabItem(
                        text = "Full",
                        isSelected = selectedModelType == MODEL_TYPE_FULL,
                        onClick = {
                            // Just switch tab, selections are stored separately
                            onModelTypeChanged(MODEL_TYPE_FULL)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // main content area
                Column(Modifier.weight(1f)) {
                    Text(
                        "Translation Mode:",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Checkbox(checked = enableOnlineMode, onCheckedChange = onOnlineModeChanged)
                        Spacer(Modifier.width(8.dp))
                        Text("Enable online translation", style = MaterialTheme.typography.bodyMedium)
                    }

                    Text(
                        "Select NLP model pairs for offline use:",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (filteredPairs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No $selectedModelType translation models available",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = filteredPairs,
                                key = { "${it.forwardModel.id}-${selectedModelType}" }
                            ) { pair ->
                                ModelPairRow(
                                    pair = pair,
                                    isSelected = isPairSelected(pair, selectedPairs),
                                    onSelectionChanged = { selected ->
                                        val forwardId = "${pair.forwardModel.id}-${pair.forwardModel.modelType}"
                                        val reverseId = "${pair.reverseModel.id}-${pair.reverseModel.modelType}"
                                        val newSelection = if (selected) {
                                            selectedPairs + forwardId + reverseId
                                        } else {
                                            selectedPairs - forwardId - reverseId
                                        }
                                        onPairsChanged(newSelection)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // nav buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onBack) { Text("Back") }
            Button(
                onClick = onNext,
                enabled = true
            ) {
                Text("Next")
            }
        }
    }
}

/* Modifiers kept drawing over nav buttons, moving this to its own composable seems to have fixed it */
@Composable
private fun ModelPairRow(
    pair: DownloadableModelPair,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChanged
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                pair.displayName,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${pair.forwardModel.modelType.uppercase()} • ${formatFileSize(pair.forwardModel.size + pair.reverseModel.size)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* -- DOWNLOAD_PAGE -- */

// TODO: make 'finish' actually work (well it does, but the app auto-finishes before the user even presses it)
@Composable
fun DownloadScreen(
    downloadProgress: Map<String, Float>,
    downloadStatus: Map<String, DownloadStatus>,
    totalModels: Int,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .height(200.dp)
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Calculate download stats
        val completed = downloadStatus.count { it.value == DownloadStatus.COMPLETED }
        val extracting = downloadStatus.count { it.value == DownloadStatus.EXTRACTING }
        val downloading = downloadStatus.count { it.value == DownloadStatus.DOWNLOADING }
        val failed = downloadStatus.count { it.value == DownloadStatus.FAILED }

        // Main text, Shows overall progress and current state
        Text(
            when {
                totalModels == 0 -> "No models selected. Setup complete!"
                completed == totalModels -> "Setup Complete"
                extracting > 0 -> "Extracting files: $completed/${totalModels + 1}"
                downloading > 0 -> "Downloading: ${completed + downloading}/${totalModels + 1}"
                failed > 0 -> "Some downloads failed, check connection"
                else -> "Preparing..."
            },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(Modifier.height(16.dp))

        // detailed stats for each model being downloaded
        if (totalModels > 0 && downloadStatus.size <= 4) {
            downloadStatus.forEach { (modelId, status) ->
                val statusText = when (status) {
                    DownloadStatus.DOWNLOADING -> {
                        val progress = downloadProgress[modelId] ?: 0f
                        "Downloading (${(progress * 100).toInt()}%)"
                    }
                    DownloadStatus.EXTRACTING -> "Extracting..."
                    DownloadStatus.COMPLETED -> "Download Successful"
                    DownloadStatus.FAILED -> "Download Failed"
                }
                Text(
                    "${modelId}: $statusText",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = when (status) {
                        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        } else if (totalModels > 0) {
            // Summary view when many models are downloading
            Text(
                "$completed ready, $downloading downloading, $extracting extracting",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp
            )
        } else {
            Text(
                "Offline NLP models can be added later from the translation screen",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onFinish,
                enabled = totalModels == 0 || completed == totalModels
            ) {
                Text("Finish")
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            Text("Loading available models...")
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                buildAnnotatedString {
                    append("Error loading models: ")
                    append(error.take(100)) // Limit display length for long errors
                    if (error.length > 100) append("...")
                },
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun TabItem( // same as the one in TranslatorScreen but without the icon
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = backgroundColor,
        contentColor = contentColor,
        modifier = modifier
            .padding(4.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

// Creates bidirectional translation pairs from individual models
// Groups by model type (lite/full) first, then matches forward and reverse models
fun generateTranslationPairs(
    remoteModels: List<DownloadableModel>,
): List<DownloadableModelPair> {
    // Group models by type first, then process each type separately
    val modelsByType = remoteModels.groupBy { it.modelType }
    val pairs = mutableListOf<DownloadableModelPair>()

    // Process each model type separately
    modelsByType.forEach { (modelType, typeModels) ->
        val processedModelIds = mutableSetOf<String>()

        // Create lookup map for this type only
        val modelMap = typeModels.associateBy { it.id }

        typeModels.forEach { forwardModel ->
            if (forwardModel.id in processedModelIds) {
                return@forEach
            }

            val reverseKey = "${forwardModel.targetLang.code}-${forwardModel.sourceLang.code}"

            val reverseModel = modelMap[reverseKey]

            if (reverseModel != null && reverseModel.modelType == modelType) {
                pairs.add(DownloadableModelPair(forwardModel, reverseModel))
                processedModelIds.add(forwardModel.id)
                processedModelIds.add(reverseModel.id)
            } else {
                // its better than just having the app die
                println("WARNING: No matching reverse model found for ${forwardModel.id} (type: $modelType)")
            }
        }
    }

    return pairs
}

// copy of the version in LangManager, generates the displayed size
fun formatFileSize(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0

    while (size > 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return "%.1f %s".format(size, units[unitIndex])
}