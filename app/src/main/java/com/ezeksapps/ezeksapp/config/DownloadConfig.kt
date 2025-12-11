package com.ezeksapps.ezeksapp.config

import kotlinx.serialization.Serializable

@Serializable
data class DownloadConfig(
    /* The base URL for the model repository */
    val baseUrl: String = "https://raw.githubusercontent.com/Ezeksapps/Android-NLP",
    /* repo base URL for files uploaded through Git-LFS */
    val lfsBaseUrl: String = "https://media.githubusercontent.com/media/Ezeksapps/Android-NLP",
    /* Subdirectory for lite models (Firefox-Bergamot) */
    val liteModelsDir: String = "lite",
    /* Subdirectory for full models */
    val fullModelsDir: String = "full",
    /* Most repos are always main anyway */
    val branchName: String = "main",
    /* git LFS uses a different URL approach, these exist so the app can know which URL system to use */
    val liteUsesLfs: Boolean = false,
    val fullUsesLfs: Boolean = true
) {
    companion object {
        const val CONFIG_FILE_NAME = "download_config.json"
    }

    /* Github (& I'm assuming other git cloud-hosting platforms) download URLs will always be in the following format:
    raw.githubusercontent.com/{username}/{repository}/{branch}/{path to file}, Full models are too big for standard upload & use
    Git LFS & therefore use a different URL system, the format for this is:
    media.githubusercontent.com/media/{username}/{repository}/refs/heads/{branch}/{path to file} */
    fun getDownloadUrl(modelType: String, modelName: String): String {
        val usesLfs = when (modelType) {
            "full" -> fullUsesLfs
            "lite" -> liteUsesLfs
            else -> false
        }

        val base = if (usesLfs) lfsBaseUrl else baseUrl

        return if (usesLfs) {
            "$base/refs/heads/$branchName/$modelType/$modelName"
        } else {
            "$base/$branchName/$modelType/$modelName"
        }
    }

}
