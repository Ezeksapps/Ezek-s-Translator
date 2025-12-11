package com.ezeksapps.ezeksapp.langmanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ezeksapps.ezeksapp.R
import com.ezeksapps.ezeksapp.model.DownloadableModel
import com.ezeksapps.ezeksapp.model.DownloadableModelPair
import com.ezeksapps.ezeksapp.model.TranslationModel
import com.ezeksapps.ezeksapp.model.TranslationModelPair
import com.ezeksapps.ezeksapp.network.DownloadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LangManagerScreen(
    onBack: () -> Unit,
    viewModel: LangManagerViewModel = hiltViewModel()
) {
    val installedModels by viewModel.installedModels.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()

    // Generate pairs using the updated logic that matches SetupScreen
    val installedPairs = remember(installedModels) {
        generateTranslationModelPairs(installedModels)
    }
    val availablePairs = remember(availableModels) {
        generateDownloadableModelPairs(availableModels)
    }

    // Separate pairs by type for better organization
    val installedLitePairs = installedPairs.filter { it.forwardModel.modelType == "lite" }
    val installedFullPairs = installedPairs.filter { it.forwardModel.modelType == "full" }
    val availableLitePairs = availablePairs.filter { it.forwardModel.modelType == "lite" }
    val availableFullPairs = availablePairs.filter { it.forwardModel.modelType == "full" }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Installed", "Available")

    // Show error dialog
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                Button(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Installed Models") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    TabItem(
                        text = title,
                        isSelected = selectedTab == index,
                        onClick = { selectedTab = index },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> InstalledModelsSection(
                        litePairs = installedLitePairs,
                        fullPairs = installedFullPairs,
                        onRemove = { pair -> viewModel.removeModelPair(pair) }
                    )
                    1 -> AvailableModelsSection(
                        litePairs = availableLitePairs,
                        fullPairs = availableFullPairs,
                        onInstall = { pair -> viewModel.installModelPair(pair) },
                        onRefresh = { viewModel.loadAvailableModels() },
                        downloadStates = downloadStates
                    )
                }
            }
        }
    }
}

@Composable
fun InstalledModelsSection(
    litePairs: List<TranslationModelPair>,
    fullPairs: List<TranslationModelPair>,
    onRemove: (TranslationModelPair) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (litePairs.isEmpty() && fullPairs.isEmpty()) {
            item {
                Text(
                    text = "No models installed",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (litePairs.isNotEmpty()) {
            item {
                Text(
                    text = "Lite Models",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(litePairs) { pair ->
                InstalledModelPairCard(
                    pair = pair,
                    onRemove = { onRemove(pair) }
                )
            }
        }

        if (fullPairs.isNotEmpty()) {
            item {
                Text(
                    text = "Full Models",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(fullPairs) { pair ->
                InstalledModelPairCard(
                    pair = pair,
                    onRemove = { onRemove(pair) }
                )
            }
        }
    }
}

@Composable
fun AvailableModelsSection(
    litePairs: List<DownloadableModelPair>,
    fullPairs: List<DownloadableModelPair>,
    onInstall: (DownloadableModelPair) -> Unit,
    onRefresh: () -> Unit,
    downloadStates: Map<String, DownloadState>
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Button(
            onClick = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Refresh Available Models")
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (litePairs.isEmpty() && fullPairs.isEmpty()) {
                item {
                    Text(
                        text = "No models available or all models are installed",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (litePairs.isNotEmpty()) {
                item {
                    Text(
                        text = "Lite Models",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(litePairs) { pair ->
                    AvailableModelPairCard(
                        pair = pair,
                        onInstall = { onInstall(pair) },
                        downloadStates = downloadStates
                    )
                }
            }

            if (fullPairs.isNotEmpty()) {
                item {
                    Text(
                        text = "Full Models",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(fullPairs) { pair ->
                    AvailableModelPairCard(
                        pair = pair,
                        onInstall = { onInstall(pair) },
                        downloadStates = downloadStates
                    )
                }
            }
        }
    }
}

@Composable
fun InstalledModelPairCard(
    pair: TranslationModelPair,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = pair.displayName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Type: ${pair.forwardModel.modelType}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onRemove) {
                Text("Remove")
            }
        }
    }
}

@Composable
fun AvailableModelPairCard(
    pair: DownloadableModelPair,
    onInstall: () -> Unit,
    downloadStates: Map<String, DownloadState>
) {
    // Use the same ID format as SetupScreen: "${model.id}-${model.modelType}"
    val forwardModelId = "${pair.forwardModel.id}-${pair.forwardModel.modelType}"
    val reverseModelId = "${pair.reverseModel.id}-${pair.reverseModel.modelType}"

    val forwardDownloading = forwardModelId in downloadStates
    val reverseDownloading = reverseModelId in downloadStates
    val isDownloading = forwardDownloading || reverseDownloading

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = pair.displayName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Type: ${pair.forwardModel.modelType} • Size: ${formatFileSize(pair.forwardModel.size + pair.reverseModel.size)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Button(onClick = onInstall) {
                        Text("Install")
                    }
                }
            }

            // Individual progress for both models
            if (isDownloading) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Forward model progress
                    if (forwardDownloading) {
                        val forwardState = downloadStates[forwardModelId]
                        Text(
                            text = "${pair.forwardModel.sourceLang.code}-${pair.forwardModel.targetLang.code}: ${getProgressText(forwardState)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Reverse model progress
                    if (reverseDownloading) {
                        val reverseState = downloadStates[reverseModelId]
                        Text(
                            text = "${pair.reverseModel.sourceLang.code}-${pair.reverseModel.targetLang.code}: ${getProgressText(reverseState)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// Helper function to get progress text
private fun getProgressText(downloadState: DownloadState?): String {
    return when (downloadState?.status) {
        DownloadStatus.DOWNLOADING -> {
            val progressPercent = (downloadState.progress * 100).toInt()
            "Downloading ($progressPercent%)"
        }
        DownloadStatus.EXTRACTING -> "Extracting..."
        DownloadStatus.FAILED -> "Download failed"
        DownloadStatus.COMPLETED -> "Completed"
        else -> "Preparing..."
    }
}

@Composable
fun TabItem(
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
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = backgroundColor,
        contentColor = contentColor,
        modifier = modifier
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            textAlign = TextAlign.Center
        )
    }
}

// Updated pair generation functions to match SetupScreen logic
fun generateDownloadableModelPairs(models: List<DownloadableModel>): List<DownloadableModelPair> {
    // Group models by type first, then process each type separately
    val modelsByType = models.groupBy { it.modelType }
    val pairs = mutableListOf<DownloadableModelPair>()

    // Process each model type separately (lite, full, etc.)
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
            }
        }
    }

    return pairs
}

fun generateTranslationModelPairs(models: List<TranslationModel>): List<TranslationModelPair> {
    // Group models by type first, then process each type separately
    val modelsByType = models.groupBy { it.modelType }
    val pairs = mutableListOf<TranslationModelPair>()

    // Process each model type separately (lite, full, etc.)
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
                pairs.add(TranslationModelPair(forwardModel, reverseModel))
                processedModelIds.add(forwardModel.id)
                processedModelIds.add(reverseModel.id)
            }
        }
    }

    return pairs
}

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