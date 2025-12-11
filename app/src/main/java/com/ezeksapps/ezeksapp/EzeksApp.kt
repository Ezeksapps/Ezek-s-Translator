package com.ezeksapps.ezeksapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/* This is Hilt's app-level dependency container, it is attached to the Application obj life cycle
* Hilt can provide di to @AndroidEntryPoint & @HiltViewModel */
@HiltAndroidApp
class EzeksApplication : Application() {}