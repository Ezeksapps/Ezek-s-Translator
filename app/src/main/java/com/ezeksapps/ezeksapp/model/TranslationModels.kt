package com.ezeksapps.ezeksapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Lang(
    val code: String,
    val name: String
)

/* Added because base Lang class can't handle model types & a data class needed to be created in order to convey the selected modelType
* to the user in the UI */
data class LangSelection(
    val lang: Lang,
    val modelType: String? = null // null for auto-detect, lite/full otherwise
) {
    // These are here so operations using this class don't look too weird (otherwise we end up with lang.lang.code in LangDropdown)
    val displayName: String = when {
        modelType == null -> lang.name
        else -> "${lang.name} (${modelType.replaceFirstChar { it.uppercase() }})"
    }
    val code: String = lang.code
}

@Serializable
data class DownloadableModel(
    val id: String,
    val sourceLang: Lang,
    val targetLang: Lang,
    val downloadUrl: String,
    val size: Long,
    val checksum: String = "",
    val modelType: String
) {
    fun toTranslationModel(isDownloaded: Boolean = false): TranslationModel {
        return TranslationModel(
            id = id,
            sourceLang = sourceLang,
            targetLang = targetLang,
            displayName = "${sourceLang.name} to ${targetLang.name}",
            modelType = modelType,
            isDownloaded = isDownloaded
        )
    }
}


data class TranslationModel(
    val id: String,
    val sourceLang: Lang,
    val targetLang: Lang,
    val displayName: String,
    val modelType: String,
    val isDownloaded: Boolean = false
) {
    fun getReverseModel(): TranslationModel {
        return TranslationModel(
            id = "${targetLang.code}-${sourceLang.code}",
            sourceLang = targetLang,
            targetLang = sourceLang,
            displayName = "${targetLang.name} to ${sourceLang.name}",
            modelType = modelType,
            isDownloaded = false
        )
    }
}

/* NOTE: non-bidirectional NLP models are not supported by the app! If you have a model of type full 'en-es.zip'
* then you also need a corresponding 'es-en.zip' of the same model type */

data class TranslationModelPair(
    val forwardModel: TranslationModel,
    val reverseModel: TranslationModel,
) {
    val displayName: String
        get() = "${forwardModel.sourceLang.name} ↔ ${forwardModel.targetLang.name}"
}

data class DownloadableModelPair(
    val forwardModel: DownloadableModel,
    val reverseModel: DownloadableModel,
) {
    val displayName: String
        get() = "${forwardModel.sourceLang.name} ↔ ${forwardModel.targetLang.name}"
}

