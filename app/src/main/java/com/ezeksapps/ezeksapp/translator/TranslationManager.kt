package com.ezeksapps.ezeksapp.translator

import android.content.Context
import com.ezeksapps.ezeksapp.bergamot.TranslationEngine
import com.ezeksapps.ezeksapp.core.utils.LangUtils
import com.ezeksapps.ezeksapp.core.utils.ModelUtils
import com.ezeksapps.ezeksapp.data.DataStoreManager
import com.ezeksapps.ezeksapp.jni.DetectionResult
import com.ezeksapps.ezeksapp.jni.LangDetectJNI
import com.ezeksapps.ezeksapp.model.Lang
import com.ezeksapps.ezeksapp.network.TranslationService
import kotlinx.coroutines.flow.first
import java.io.File

/* acts as a common interface between either offline or online translation */

class TranslationManager(
    context: Context,
    private val dataStoreManager: DataStoreManager,
    private val translationService: TranslationService,
    private val modelUtils: ModelUtils
) {
    val modelDir = File(context.getExternalFilesDir(null), "models")
    private val engine = TranslationEngine(context)
    private val langDetector = LangDetectJNI()
    private lateinit var detectedLang: DetectionResult
    @Volatile private var lastLangPair: Pair<String, String>? = null
    @Volatile private var currentModelType: String? = null

    /* only fetches online translation langs, cannot be used for local NLP because the
     Lang class & therefore the function can't handle modelType */
    suspend fun getLangs(): List<Lang> {
        val isOnline = dataStoreManager.onlineModeEnabled.first()
        return if (isOnline) {
            translationService.getLangs()
        } else {
            throw Exception("Cannot invoke getLangs() when online mode is disabled")
        }
    }

    /* Before I has the idea of having separate Lite & Full model options, translate() only had one version,
    * I did try such an implementation when adding modelTypes, but the code quickly became hieroglyphics,
    * translate() now has two versions where one is for online translation & the other for offline, this way
    * we don't have modelType ending up as a redundant param when handling online translation.
    * If you run the version of translate() without modelType, the code will not check for anything, it will
    * just assume you want to perform online translation, & vice versa */

    suspend fun translate(text: String, source: String, target: String): String { // online version
        return translationService.translate(text, source, target)
    }

    // I think this illustrates how much more complex local translation actually is

    suspend fun translate(text: String, source: String, target: String, modelType: String): String { // offline version
        var modelKey = "$source-$target"
        var actualSource = source // track actual source language for model checking

        // handle auto-detect through CLD2
        if (source == "auto") {
            detectedLang = langDetector.detectLang(text)
            if (!LangUtils.isLangSupported(detectedLang.lang)) { // should I make the lang support list dynamic?
                throw Exception("Language detected has no offline model available to perform translation")
            }
            modelKey = "${detectedLang.lang}-$target"
            actualSource = detectedLang.lang
        }

        // Check if the required model is installed using ModelUtils
        val installedModels = modelUtils.getInstalledModels()
        val modelExists = installedModels.any { (modelId, modelTypes) ->
            modelId == modelKey && modelTypes.contains(modelType)
        }

        if (!modelExists) {
            val fromLangName = if (source == "auto") {
                val detectedName = LangUtils.getLangName(detectedLang.lang)
                "$detectedName (detected)"
            } else {
                LangUtils.getLangName(source)
            }
            val toLangName = LangUtils.getLangName(target)
            throw Exception("$fromLangName → $toLangName $modelType model not installed. Download it from Manage Languages.")
        }

        // only restart & init engine if lang pair selection changes, model type changes or no engine instance is active
        if (lastLangPair != Pair(actualSource, target) || currentModelType != modelType || !engine.isReady) {
            engine.shutdown() // clear any old data

            /* AS compacted the code, but affected readability a bit, this just runs different versions of setup
            * depending on if source is auto or not & Boolean success will be set to true if either of the operations
            * is successful  */
            val success: Boolean = if (source == "auto") {
                engine.setup(modelDir, detectedLang.lang, target, modelType)
            } else {
                engine.setup(modelDir, source, target, modelType)
            }
            if (!success) throw Exception("Failed to load offline model for $modelKey")
            // update values
            lastLangPair = Pair(actualSource, target)
            currentModelType = modelType
        }

        return engine.translate(text) // run engine translation after init operations
    }

    fun shutdown() {
        engine.shutdown()
    }
}