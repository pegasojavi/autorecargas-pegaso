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
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

/**
 * Selector de idioma (CLAUDE.md sección 6): el desplegable se genera
 * parseando directamente `res/xml/locales_config.xml`, no desde
 * `resources.assets.locales` como se planteó originalmente — esa API
 * devuelve TODOS los locales para los que hay algún recurso en el APK
 * final, incluidos los que traen las propias librerías (AppCompat/Material
 * traducen sus textos internos a 60-90 idiomas), no solo los nuestros. Con
 * eso el selector mostraba el listado completo de idiomas del mundo (bug
 * real detectado en dispositivo). No se usa `LocaleManagerCompat` porque
 * esa clase (androidx.core 1.18.0) solo expone `getSystemLocales`/
 * `getApplicationLocales` — no existe ningún `getApplicationSupportedLocales`
 * que lea el `locale-config` declarado, así que hay que parsear el XML a
 * mano. `locales_config.xml` sigue siendo una única fuente de verdad
 * declarativa — añadir un idioma es añadir una línea ahí más su
 * `values-<lang>/strings.xml`, no tocar este código.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val availableLocales: List<Locale> = remember {
        val result = mutableListOf<Locale>()
        val parser = context.resources.getXml(R.xml.locales_config)
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                    val tag = parser.getAttributeValue(ANDROID_XML_NAMESPACE, "name")
                    if (tag != null) result.add(Locale.forLanguageTag(tag))
                }
                eventType = parser.next()
            }
        } finally {
            parser.close()
        }
        result.distinctBy { locale: Locale -> locale.toLanguageTag() }
            .sortedBy { locale: Locale -> locale.getDisplayName(locale) }
    }

    var selectedTag by remember {
        mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotBlank() })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
                    headlineContent = { Text(stringResource(R.string.settings_system_language)) },
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

private const val ANDROID_XML_NAMESPACE = "http://schemas.android.com/apk/res/android"
