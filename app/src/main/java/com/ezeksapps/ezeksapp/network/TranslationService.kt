package com.ezeksapps.ezeksapp.network

import android.content.Context
import com.ezeksapps.ezeksapp.model.Lang
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.url
import kotlinx.serialization.json.Json
import java.net.URLEncoder


/* Currently this uses only Lingva as its translation service, Lingva is like NewPipe but for Google Translate in that
it allows API calls to be made without the tracking or data logging. LibreTranslate was the original choice but that requires
a paid API key which I cant afford (this is a free app) & I do not have the facilities to host my own instance,
if anyone looking through this code has any suggestions on other services please let me know

TODO: add a setting that allows the user to set their own instances */

class TranslationService(context: Context? = null) {
    private val client = KtorClient.instance
    private val json = Json { ignoreUnknownKeys = true }
    private val instances = listOf("https://lingva.ml", "https://lingva.lunar.icu")

    suspend fun getLangs() = instances.firstNotNullOfOrNull { instance ->
        try {
            client.get { url("$instance/api/v1/languages") }
                .body<Map<String, List<Lang>>>()["languages"]
        } catch (e: Exception) { null }
    } ?: listOf(Lang("en", "English"), Lang("es", "Spanish"))

    suspend fun translate(text: String, source: String, target: String): String {
        if (text.isBlank()) return ""

        val cleanSource = if (source.isBlank() || source == "auto") "auto" else source

        return instances.firstNotNullOfOrNull { instance ->
            try {
                client.get {
                    url("${instance.removeSuffix("/")}/api/v1/$cleanSource/$target/" +
                            URLEncoder.encode(text, "UTF-8").replace("+", "%20"))
                }.body<String>().let { response ->
                    when {
                        response.startsWith("{") ->
                            json.decodeFromString<Map<String, String>>(response)["translation"]
                        else -> response
                    }
                }
            } catch (e: Exception) { null }
        } ?: throw Exception("Translation service unavailable")
    }
}