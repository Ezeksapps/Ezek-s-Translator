package com.ezeksapps.ezeksapp.bergamot

import android.content.Context
import com.ezeksapps.ezeksapp.jni.TranslatorJNI
import java.io.File

class TranslationEngine(private val context: Context) {

    private var nativePtr: Long = 0
    private var modelReady = false
    private val translatorJNI = TranslatorJNI()

    val isReady: Boolean get() = modelReady

    companion object {
        init {
            System.loadLibrary("translator_jni")
        }
    }

    /* TODO: C++ init() fails on full models, fix */

    fun setup(modelDir: File, sourceLang: String, targetLang: String, modelType: String): Boolean {
        return try {
            // file inception (forgive me, I am new to kotlin, I know this looks cursed)
            val langPairDir = File(File(modelDir, modelType), "$sourceLang-$targetLang")

            if (!langPairDir.exists()) {
                return false
            }

            nativePtr = translatorJNI.createNativeInstance() // TODO: fix these terrible JNI names (just make them the original C++ names)
            if (nativePtr == 0L) {
                return false
            }

            // Init with model directory
            modelReady = translatorJNI.initNativeEngine(nativePtr, langPairDir.absolutePath)

            modelReady // this is the return value (This comment is only here because i will forget this is return try not try)
        } catch (e: Exception) {
            throw Exception("Translation Engine setup failed, ${e.message}")
        }
    }

    fun translate(text: String): String {
        if (!modelReady || nativePtr == 0L) {
            throw IllegalStateException("Engine not ready")
        }

        if (text.trim().isEmpty()) return "" // because you cant translate blank space (obviously)

        return try {
            translatorJNI.runTranslation(nativePtr, text)
        } catch (e: Exception) {
            throw Exception("Translation error: ${e.message}")
        }
    }

    fun shutdown() {
        if (nativePtr != 0L) {
            translatorJNI.deleteNativeInstance(nativePtr)
            nativePtr = 0
        }
        modelReady = false
    }
}