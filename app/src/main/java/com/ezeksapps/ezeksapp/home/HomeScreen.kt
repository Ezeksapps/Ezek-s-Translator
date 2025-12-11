package com.ezeksapps.ezeksapp.home


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ezeksapps.ezeksapp.R


// THIS IS FOR THE TOP APP BAR API, REMOVE IF DOESN'T BECOME STABLE OR IS REMOVED
@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun HomeScreen(
    onCameraTapped: () -> Unit,
    onTranslatorTapped: () -> Unit,
    onPhrasebookTapped: () -> Unit,
    onAppInfoTapped: () -> Unit
) {
    // Scaffold provides top-level layout structure
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Translator",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Extended FABs for primary actions
            ExtendedFloatingActionButton(
                onClick = onTranslatorTapped,
                modifier = Modifier.fillMaxWidth(),
                icon = { Icon(
                    painterResource(R.drawable.translate),
                    "Translate"
                ) },
                text = { Text("Text Translator") }
            )

            ExtendedFloatingActionButton(
                onClick = onCameraTapped,
                modifier = Modifier.fillMaxWidth(),
                icon = { Icon(
                    painterResource(R.drawable.camera),
                    "Camera"
                ) },
                text = { Text("Camera Translator") }
            )

            ExtendedFloatingActionButton(
                onClick = onPhrasebookTapped,
                modifier = Modifier.fillMaxWidth(),
                icon = { Icon(
                    painterResource(R.drawable.phrasebook),
                    "Phrasebook"
                ) },
                text = { Text("Phrasebook") }
            )
            ExtendedFloatingActionButton(
                onClick = onAppInfoTapped,
                modifier = Modifier.fillMaxWidth(),
                icon = { Icon(
                    painterResource(R.drawable.info_log),
                    "App Info"
                ) },
                text = { Text("App Info") }
            )
        }
    }
}
