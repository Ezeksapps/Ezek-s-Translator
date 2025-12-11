package com.ezeksapps.ezeksapp.translator

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ezeksapps.ezeksapp.R
import com.ezeksapps.ezeksapp.model.LangSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    onBack: () -> Unit,
    onManageLangs: () -> Unit,
    viewModel: TranslatorScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val langs by viewModel.langs.collectAsState() // List<LangSelection> not List<Lang>
    val isLoading by viewModel.isLoading.collectAsState()
    val translationResult by viewModel.translationResult.collectAsState()
    val isOnline by viewModel.isOnlineMode.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val sourceLang by viewModel.sourceLang.collectAsState()
    val targetLang by viewModel.targetLang.collectAsState()


    /* NOTE: originally translateText() took the source & target lang codes as params,
    * it is just the text as a param now, source & target langs will be set on click through
    * the LangDropdowns */

    var inputText by remember { mutableStateOf("") }

    /* I've got to say... Kotlin has the weirdest syntax ever. All this because it complains about non-nullables */
    fun LangSelection?.canSwap(): Boolean = this != null && this.code != "auto" // extension function to LangSelection class
    val onSwapLangs = {
        if (sourceLang?.canSwap() == true && targetLang != null) {
            viewModel.setSourceLang(targetLang!!)
            viewModel.setTargetLang(sourceLang!!)
        } else {
            Toast.makeText(context, "Cannot swap with auto-detect", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Translator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        TabItem(
                            iconRes = R.drawable.offline,
                            text = "Offline",
                            isSelected = !isOnline,
                            onClick = { viewModel.setOnlineMode(false) }
                        )
                        TabItem(
                            iconRes = R.drawable.online,
                            text = "Online",
                            isSelected = isOnline,
                            onClick = { viewModel.setOnlineMode(true) }
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (!isOnline) {
                Button(
                    onClick = onManageLangs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Manage Languages")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LangDropdown(
                    selectedLang = sourceLang,
                    langs = langs,
                    onLangSelected = { selectedLang ->
                        viewModel.setSourceLang(selectedLang)
                    },
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onSwapLangs) {
                    Icon(
                        painter = painterResource(R.drawable.swap_horiz),
                        contentDescription = "Swap languages"
                    )
                }

                LangDropdown(
                    selectedLang = targetLang,
                    langs = langs,
                    onLangSelected = { selectedLang ->
                        viewModel.setTargetLang(selectedLang)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Enter text") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Button(
                    onClick = {
                        viewModel.translateText(
                            text = inputText,
                        )
                    },
                    enabled = !isLoading && inputText.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Translate")
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                if (!isLoading && translationResult.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = translationResult,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        )
                    }
                }

                // Error message
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun TabItem(
    iconRes: Int,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = backgroundColor,
        contentColor = contentColor,
        modifier = Modifier
            .padding(4.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = text,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}