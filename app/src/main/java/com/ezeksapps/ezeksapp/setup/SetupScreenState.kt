package com.ezeksapps.ezeksapp.setup

data class SetupScreenState(
    val selectedLang: String = "en",
    val enableOnlineMode: Boolean = true,
    val currentPage: Int = 0,
    val hasDownloadTriggered: Boolean = false
)