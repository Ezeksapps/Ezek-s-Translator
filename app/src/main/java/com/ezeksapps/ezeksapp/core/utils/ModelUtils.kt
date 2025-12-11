package com.ezeksapps.ezeksapp.core.utils

import android.content.Context
import java.io.File

/* Deals specifically with the local NLP models only, this is different to LangUtils in that the util funcs here handle modelType
* It is a class instead of an object because of this thing we need called a context that likes to make my life hard for no reason at all */

class ModelUtils (private val context: Context) {
    private fun getRootModelDir(): File {
        return File(context.getExternalFilesDir(null), "models").apply {
            if (!exists()) mkdirs()
        }
    }

    fun getModelDir(modelId: String, modelType: String): File {
        val typeDir = when (modelType) {
            "lite" -> File(getRootModelDir(), "lite")
            "full" -> File(getRootModelDir(), "full")
            else -> throw IllegalArgumentException("Invalid model type: $modelType")
        }
        return File(typeDir, modelId).apply {
            if (!exists()) mkdirs()
        }
    }

    fun getInstalledModels(): Map<String, List<String>> {
        val modelsDir = getRootModelDir()
        val installedModels = mutableMapOf<String, MutableList<String>>()

        // Check lite directory
        val liteDir = File(modelsDir, "lite")
        if (liteDir.exists()) {
            liteDir.listFiles()?.forEach { modelDir ->
                if (modelDir.isDirectory && modelDir.listFiles()?.isNotEmpty() == true) {
                    installedModels.getOrPut(modelDir.name) { mutableListOf() }.add("lite")
                }
            }
        }

        // Check full directory
        val fullDir = File(modelsDir, "full")
        if (fullDir.exists()) {
            fullDir.listFiles()?.forEach { modelDir ->
                if (modelDir.isDirectory && modelDir.listFiles()?.isNotEmpty() == true) {
                    installedModels.getOrPut(modelDir.name) { mutableListOf() }.add("full")
                }
            }
        }

        return installedModels
    }
}