package com.ezeksapps.ezeksapp.jni

class TranslatorJNI {

    external fun createNativeInstance(): Long
    external fun initNativeEngine(nativePtr: Long, modelDir: String): Boolean
    external fun runTranslation(nativePtr: Long, text: String): String
    external fun deleteNativeInstance(nativePtr: Long)

    companion object {
        init {
            System.loadLibrary("translator_jni")
        }
    }
}