package com.ezeksapps.ezeksapp.core.utils

object LangUtils {
    private val supportedLangs = mapOf(
        "en" to "English",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "it" to "Italian",
        "pt" to "Portuguese",
        "ru" to "Russian",
        "ja" to "Japanese",
        "ko" to "Korean",
        "zh" to "Chinese",
        "ar" to "Arabic",
        "hi" to "Hindi",
        "bg" to "Bulgarian",
        "ca" to "Catalan",
        "cs" to "Czech",
        "et" to "Estonian",
        "fi" to "Finnish",
        "hu" to "Hungarian",
        "is" to "Icelandic",
        "lt" to "Lithuanian",
        "lv" to "Latvian",
        "nl" to "Dutch",
        "pl" to "Polish",
        "sk" to "Slovak",
        "sl" to "Slovenian",
        "uk" to "Ukrainian",
        "auto" to "Detect Language"
    )

    fun getLangName(code: String): String {
        return supportedLangs[code] ?: code
    }

    fun isLangSupported(code: String): Boolean {
        return supportedLangs.containsKey(code)
    }
}