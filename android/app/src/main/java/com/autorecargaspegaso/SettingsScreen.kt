package com.autorecargaspegaso

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Selector de idioma (CLAUDE.md sección 6): el desplegable se genera solo a
 * partir de los ficheros de traducción realmente empaquetados
 * (`resources.assets.locales`), nunca de una lista escrita a mano — así no
 * se puede desincronizar de los idiomas que la app tiene de verdad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val availableLocales = remember {
        context.assets.locales
            .filter { it.isNotBlank() && it != "und" }
            .mapNotNull { tag -> runCatching { Locale.forLanguageTag(tag) }.getOrNull() }
            .distinctBy { it.toLanguageTag() }
            .sortedBy { it.getDisplayName(it) }
    }

    var selectedTag by remember {
        mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotBlank() })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Idioma") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(padding)) {
            item {
                ListItem(
                    headlineContent = { Text("Idioma del sistema") },
                    leadingContent = { RadioButton(selected = selectedTag == null, onClick = null) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedTag = null
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                    },
                )
            }
            items(availableLocales, key = { it.toLanguageTag() }) { locale ->
                val tag = locale.toLanguageTag()
                ListItem(
                    headlineContent = { Text(locale.getDisplayName(locale).replaceFirstChar { it.uppercase() }) },
                    leadingContent = { RadioButton(selected = selectedTag == tag, onClick = null) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedTag = tag
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    },
                )
            }
        }
    }
}
