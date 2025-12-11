package com.ezeksapps.ezeksapp.navigation

sealed class Destinations(val route: String) {
    object Home : Destinations("home")
    object Camera : Destinations("camera")
    object Translator : Destinations("translator")
    object Phrasebook : Destinations("phrasebook")
    object AppInfo : Destinations("app_info")
    object LangManager : Destinations("language_manager")
}