package com.ezeksapps.ezeksapp.config

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

/* Most apps using a download service are bound to a specific download URL or specific repo that could go down at any time
* I do not like the idea that part of an app's core functionality can be rendered useless at any time as a result of websites going down,
* cloud servers corrupting & all other possible complications of cloud repo hosting. The app by default will set this config to my github repo
* that hosts the NLP models in the format the app expects. The config exists so that any user can create their own similarly formatted
* repo on any platform that follows the standard git URL form if the main repo goes down or if they want to experiment with their own NLP models */

class ConfigManager(private val context: Context) {

    val configDir: File by lazy {
        File(context.getExternalFilesDir(null), "config").apply {
            mkdirs()
        }
    }

    private val configFile: File by lazy {
        File(configDir, DownloadConfig.CONFIG_FILE_NAME)
    }

    private val json = Json {
        prettyPrint = true // aka. format JSON from hieroglyphics to readably formatted
        ignoreUnknownKeys = true // ignore any key-value pairs the app doesn't expect
        encodeDefaults = true // write the defaults from DownloadConfig.kt
    }

    fun getConfig(): DownloadConfig {
        return if (configFile.exists()) {
            try {
                val configText = configFile.readText()
                if (configText.isBlank()) {
                    createDefaultConfig()
                } else {
                    json.decodeFromString<DownloadConfig>(configText)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                createDefaultConfig()
            }
        } else {
            createDefaultConfig()
        }
    }

    private fun createDefaultConfig(): DownloadConfig {
        val defaultConfig = DownloadConfig()
        saveConfig(defaultConfig)
        return defaultConfig
    }

    fun saveConfig(config: DownloadConfig): Boolean {
        return try {
            configFile.writeText(json.encodeToString(config))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}
