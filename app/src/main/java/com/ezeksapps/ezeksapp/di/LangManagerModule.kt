package com.ezeksapps.ezeksapp.di

import android.content.Context
import com.ezeksapps.ezeksapp.core.utils.ModelUtils
import com.ezeksapps.ezeksapp.data.DataStoreManager
import com.ezeksapps.ezeksapp.network.TranslationService
import com.ezeksapps.ezeksapp.translator.TranslationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/* Only exists, because file ops done by ModelUtils need a context for some reason */

@Module
@InstallIn(SingletonComponent::class)
object TranslationModule {

    @Provides
    @Singleton
    fun provideTranslationManager(
        @ApplicationContext context: Context,
        dataStoreManager: DataStoreManager,
        translationService: TranslationService,
        modelUtils: ModelUtils
    ): TranslationManager {
        return TranslationManager(
            context = context,
            dataStoreManager = dataStoreManager,
            translationService = translationService,
            modelUtils = modelUtils
        )
    }
}