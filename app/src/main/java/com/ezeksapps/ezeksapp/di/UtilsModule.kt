package com.ezeksapps.ezeksapp.di

import android.content.Context
import com.ezeksapps.ezeksapp.core.utils.ModelUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilsModule {

    @Provides
    @Singleton
    fun provideModelUtils(@ApplicationContext context: Context): ModelUtils {
        return ModelUtils(context)
    }
}