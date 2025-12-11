package com.ezeksapps.ezeksapp.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.gson.gson

object KtorClient {
    val instance by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                gson()
            }
            install(Logging) {
                level = LogLevel.HEADERS
            }
            engine {
                connectTimeout = 30_000
                socketTimeout = 30_000
            }
        }
    }
}