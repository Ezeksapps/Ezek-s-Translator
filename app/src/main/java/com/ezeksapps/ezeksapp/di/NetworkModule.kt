package com.ezeksapps.ezeksapp.di

import android.content.Context
import com.ezeksapps.ezeksapp.config.ConfigManager
import com.ezeksapps.ezeksapp.network.ModelService
import com.ezeksapps.ezeksapp.network.TranslationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                level = LogLevel.ALL
            }
        }
    }

    @Provides
    @Singleton
    fun provideConfigManager(@ApplicationContext context: Context): ConfigManager {
        return ConfigManager(context)
    }

    @Provides
    @Singleton
    fun provideTranslationService(@ApplicationContext context: Context): TranslationService {
        return TranslationService(context)
    }

    @Provides
    @Singleton
    fun provideModelService(
        client: HttpClient,
        configManager: ConfigManager,
        @ApplicationContext context: Context
    ): ModelService {
        return ModelService(client, configManager, context)
    }
}