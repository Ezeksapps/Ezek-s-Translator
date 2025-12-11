package com.ezeksapps.ezeksapp.jni

data class DetectionResult(
    val lang: String,
    val isReliable: Boolean,
    val confidence: Int
)

class LangDetectJNI {

    // This function signature must match your JNI function
    external fun detectLang(text: String, languageHint: String? = null): DetectionResult

    companion object {
        init {
            System.loadLibrary("lang_detect_jni")
        }
    }
}