package com.ezeksapps.ezeksapp.translator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ezeksapps.ezeksapp.R
import com.ezeksapps.ezeksapp.model.LangSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LangDropdown(
    selectedLang: LangSelection?, // nullable because Kotlin is an annoying language
    langs: List<LangSelection>, // 'langs' is not really a good name for this because it isn't a List<Lang> but I can't think of anything better
    onLangSelected: (LangSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.wrapContentSize()) {
        // Clickable row that shows the current selection
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedLang?.displayName ?: "UNDEFINED",  // this should never be empty
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            Icon(
                painter = painterResource(R.drawable.arrow_drop_down),
                contentDescription = "Lang dropdown", // I don't need alt text, why is it complaining if I remove it?
                modifier = Modifier.size(24.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
           langs.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.displayName) }, // 'lang' is actually a LangSelection not a Lang
                    onClick = {
                        onLangSelected(lang)
                        expanded = false
                    }
                )
            }
        }
    }
}