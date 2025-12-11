package com.ezeksapps.ezeksapp.network

import android.content.Context
import com.ezeksapps.ezeksapp.config.ConfigManager
import com.ezeksapps.ezeksapp.config.DownloadConfig
import com.ezeksapps.ezeksapp.core.utils.ChecksumUtils
import com.ezeksapps.ezeksapp.core.utils.LangUtils
import com.ezeksapps.ezeksapp.model.DownloadableModel
import com.ezeksapps.ezeksapp.model.Lang
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

data class DownloadProgress(
    val modelId: String,
    val progress: Float,
    val status: DownloadStatus
)

enum class DownloadStatus {
    DOWNLOADING,
    EXTRACTING,
    COMPLETED,
    FAILED
}

@Serializable
data class ModelInfo(
    val checksum: String,
    val size: Long
)

@Serializable
data class ModelMetadata(
    val lite: Map<String, ModelInfo>,
    val full: Map<String, ModelInfo> = emptyMap()
)

class ModelService(
    private val client: HttpClient,
    private val configManager: ConfigManager,
    private val context: Context
) {
    @Serializable
    private data class CachedManifest(
        val models: List<DownloadableModel>,
        val timestamp: Long
    )

    private companion object {
        const val CACHE_DURATION_MS = 86400000 // 24 hours
        const val CACHE_FILE_NAME = "models_cache.json"
        const val BUFFER_SIZE = 8192
    }

    private val cacheFile: File by lazy {
        File(configManager.configDir, CACHE_FILE_NAME)
    }

    private val _activeDownloads = mutableMapOf<String, DownloadProgress>()

    private fun getRootModelDir(): File {
        return File(context.getExternalFilesDir(null), "models").apply {
            if (!exists()) mkdirs()
        }
    }

    /* TODO: FIX THIS FILE!!! (FILTERING IS BROKEN) */

    // get the specific final directory for a given model
    private fun getModelDir(modelId: String, modelType: String): File {
        val typeDir = when (modelType) {
            "lite" -> File(getRootModelDir(), "lite")
            "full" -> File(getRootModelDir(), "full")
            else -> throw IllegalArgumentException("Invalid model type: $modelType")
        }
        return File(typeDir, modelId).apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun getAvailableModels(): List<DownloadableModel> {
        loadFromCache()?.let { return it }
        val models = fetchModelsFromMetadata()
        saveToCache(models)
        return models
    }


    private suspend fun fetchModelsFromMetadata(): List<DownloadableModel> {
        return withContext(Dispatchers.IO) {
            val config = configManager.getConfig()
            val metadataUrl = "${config.baseUrl}/${config.branchName}/metadata.json"

            try {
                val response: HttpResponse = client.get(metadataUrl)
                if (response.status.value !in 200..299) {
                    throw Exception("Failed to fetch metadata: ${response.status}")
                }

                val metadataText = response.body<String>()
                val metadata = Json.decodeFromString<ModelMetadata>(metadataText)

                val models = mutableListOf<DownloadableModel>()

                // Process lite models
                metadata.lite.forEach { (langPair, modelInfo) ->
                    val model = createDownloadableModel(
                        langPair = langPair,
                        modelInfo = modelInfo,
                        config = config,
                        modelType = "lite"
                    )
                    models.add(model)
                }

                // Process full models
                metadata.full.forEach { (langPair, modelInfo) ->
                    val model = createDownloadableModel(
                        langPair = langPair,
                        modelInfo = modelInfo,
                        config = config,
                        modelType = "full"
                    )
                    models.add(model)
                }

                models
            } catch (e: Exception) {
                throw Exception("Failed to fetch models metadata: ${e.message}")
            }
        }
    }

    private fun createDownloadableModel(
        langPair: String,
        modelInfo: ModelInfo,
        config: DownloadConfig,
        modelType: String
    ): DownloadableModel {
        val codes = langPair.split("-")
        if (codes.size != 2) {
            throw IllegalArgumentException("Invalid language pair format: $langPair")
        }

        val sourceLang = codes[0]
        val targetLang = codes[1]

        val downloadUrl = config.getDownloadUrl(modelType, "${langPair}.zip")

        return DownloadableModel(
            id = langPair,
            sourceLang = Lang(sourceLang, LangUtils.getLangName(sourceLang)),
            targetLang = Lang(targetLang, LangUtils.getLangName(targetLang)),
            downloadUrl = downloadUrl,
            size = modelInfo.size,
            checksum = modelInfo.checksum,
            modelType = modelType
        )
    }

    private suspend fun loadFromCache(): List<DownloadableModel>? {
        return withContext(Dispatchers.IO) {
            if (!cacheFile.exists()) return@withContext null

            try {
                val cached = Json.decodeFromString<CachedManifest>(cacheFile.readText())
                if (System.currentTimeMillis() - cached.timestamp < CACHE_DURATION_MS) {
                    cached.models
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun saveToCache(models: List<DownloadableModel>) {
        withContext(Dispatchers.IO) {
            val cachedManifest = CachedManifest(models, System.currentTimeMillis())
            cacheFile.writeText(Json.encodeToString(cachedManifest))
        }
    }

    suspend fun downloadModel(
        model: DownloadableModel,
        onProgress: (DownloadProgress) -> Unit
    ) {
        val modelId = model.id
        val modelDir = getModelDir(modelId, model.modelType)
        val tempZipFile = File(context.cacheDir, "${modelId}.zip")

        try {
            updateProgress(modelId, 0f, DownloadStatus.DOWNLOADING, onProgress)
            downloadModelZip(model, tempZipFile) { progress ->
                updateProgress(modelId, progress, DownloadStatus.DOWNLOADING, onProgress)
            }

            updateProgress(modelId, 1f, DownloadStatus.EXTRACTING, onProgress)
            extractZipFile(tempZipFile,  File(getRootModelDir(), model.modelType))

            tempZipFile.delete()
            updateProgress(modelId, 1f, DownloadStatus.COMPLETED, onProgress)
        } catch (e: Exception) {
            // Clean up on error
            tempZipFile.delete()
            modelDir.deleteRecursively()
            updateProgress(modelId, 0f, DownloadStatus.FAILED, onProgress)
            throw e
        } finally {
            _activeDownloads.remove(modelId)
        }
    }

    private fun updateProgress(
        modelId: String,
        progress: Float,
        status: DownloadStatus,
        onProgress: (DownloadProgress) -> Unit
    ) {
        val downloadProgress = DownloadProgress(modelId, progress, status)
        _activeDownloads[modelId] = downloadProgress
        onProgress(downloadProgress)
    }

    private suspend fun downloadModelZip(model: DownloadableModel, destination: File, progressCallback: (Float) -> Unit) {
        withContext(Dispatchers.IO) {
            val connection = URL(model.downloadUrl).openConnection() as HttpURLConnection
            try {
                connection.connect()

                if (connection.responseCode !in 200..299) {
                    throw Exception("Download failed: ${connection.responseCode}")
                }

                val contentLength = connection.contentLength.toLong().takeIf { it > 0 } ?: model.size
                var downloadedBytes = 0L

                destination.parentFile?.mkdirs()

                connection.inputStream.use { inputStream ->
                    destination.outputStream().use { outputStream ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            if (contentLength > 0) {
                                val progress = downloadedBytes.toFloat() / contentLength
                                progressCallback(progress.coerceIn(0f, 1f))
                            }
                        }
                    }
                }

                // Verify download
                val actualSize = destination.length()
                if (actualSize != model.size) {
                    throw Exception("Download size mismatch. Expected: ${model.size}, Got: $actualSize")
                }

                if (model.checksum.isNotEmpty()) {
                    verifyChecksum(destination, model.checksum)
                }

            } finally {
                connection.disconnect()
            }
        }
    }

    private suspend fun extractZipFile(zipFile: File, destinationDir: File) {
        withContext(Dispatchers.IO) {
            ZipInputStream(zipFile.inputStream()).use { zipInputStream ->
                var entry = zipInputStream.nextEntry
                while (entry != null) {
                    val file = File(destinationDir, entry.name)

                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { outputStream ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var length: Int
                            while (zipInputStream.read(buffer).also { length = it } > 0) {
                                outputStream.write(buffer, 0, length)
                            }
                        }
                    }

                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
            }
        }
    }

    private fun verifyChecksum(file: File, expectedChecksum: String) {
        val actualChecksum = ChecksumUtils.calculateSHA256(file)
        if (actualChecksum != expectedChecksum) {
            throw Exception("Checksum verification failed. Expected: $expectedChecksum, Got: $actualChecksum")
        }
    }

}