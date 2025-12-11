package com.ezeksapps.ezeksapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ezeksapps.ezeksapp.core.theme.AndroidAppTheme
import com.ezeksapps.ezeksapp.navigation.AppNavigation
import com.ezeksapps.ezeksapp.setup.SetupScreen
import com.ezeksapps.ezeksapp.setup.SetupViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            // Handle denied permissions (I won't, submit to my will!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AndroidAppTheme {
                PermissionHandler {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        SetupScreenContent()
                    }
                }
            }
        }
    }

    @Composable
    private fun SetupScreenContent() {
        val viewModel: SetupViewModel = viewModel()
        val isLoading by viewModel.isLoading.collectAsState()
        val isSetupComplete by viewModel.isSetupComplete.collectAsState()

        if (isLoading) {
            LoadingScreen()
        } else if (!isSetupComplete) {
            SetupScreen(
                onSetupComplete = {}
            )
        } else {
            AppNavigation()
        }
    }

    @Composable
    private fun LoadingScreen() {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading...",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }


    @Composable
    private fun PermissionHandler(content: @Composable () -> Unit) {
        val lifecycle = LocalLifecycleOwner.current.lifecycle

        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    requestPermissions()
                }
            }
            lifecycle.addObserver(observer)

            onDispose {
                lifecycle.removeObserver(observer)
            }
        }

        content()
    }

    private fun requestPermissions() { // android trying not to overcomplicate permission logic (impossible)
        val permissions = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    add(Manifest.permission.READ_MEDIA_IMAGES)
                }
                else -> {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }
}