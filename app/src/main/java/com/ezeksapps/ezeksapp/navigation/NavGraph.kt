package com.ezeksapps.ezeksapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ezeksapps.ezeksapp.appinfo.AppInfoScreen
import com.ezeksapps.ezeksapp.camera.CameraScreen
import com.ezeksapps.ezeksapp.home.HomeScreen
import com.ezeksapps.ezeksapp.langmanager.LangManagerScreen
import com.ezeksapps.ezeksapp.phrasebook.PhrasebookScreen
import com.ezeksapps.ezeksapp.translator.TranslatorScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.Home.route
    ) {
        composable(Destinations.Home.route) {
            HomeScreen(
                onCameraTapped = { navController.navigate(Destinations.Camera.route) },
                onTranslatorTapped = { navController.navigate(Destinations.Translator.route) },
                onPhrasebookTapped = { navController.navigate(Destinations.Phrasebook.route) },
                onAppInfoTapped = { navController.navigate(Destinations.AppInfo.route) }
            )
        }

        composable(Destinations.Camera.route) {
            CameraScreen(
                onBack = navController::popBackStack,
                onPhotoTaken = { uri ->
                    // Handle the captured photo URI
                    // For example: navigate to a preview screen
                    navController.navigate("preview/$uri")
                }
            )
        }

        composable(Destinations.Translator.route) {
            TranslatorScreen(
                onBack = navController::popBackStack,
                onManageLangs = {
                    navController.navigate(Destinations.LangManager.route)
                }
            )
        }

        composable(Destinations.Phrasebook.route) {
            PhrasebookScreen(
                onBack = navController::popBackStack
            )
        }

        composable(Destinations.AppInfo.route) {
            AppInfoScreen(
                onBack = navController::popBackStack
            )
        }


        composable(Destinations.LangManager.route) {
            LangManagerScreen(
                onBack = navController::popBackStack
            )
        }
    }
}