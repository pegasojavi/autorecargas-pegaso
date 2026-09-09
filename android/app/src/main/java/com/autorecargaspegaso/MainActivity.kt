package com.autorecargaspegaso

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.autorecargaspegaso.ui.theme.AutoRecargasPegasoTheme

/**
 * Hereda de [AppCompatActivity], no de `ComponentActivity` (plantilla
 * original) — bug real detectado en dispositivo: sin `AppCompatActivity`,
 * `AppCompatDelegate.setApplicationLocales()` (selector de idioma, CLAUDE.md
 * sección 6) guarda la preferencia pero nunca recompone la Activity con el
 * nuevo idioma — "elijas el que elijas, no traduce". `AppCompatActivity` es
 * quien engancha el ciclo de vida necesario para que el cambio de idioma
 * se aplique de verdad. Sigue siendo compatible con Compose sin cambios.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            AutoRecargasPegasoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainNavigation()
                }
            }
        }
    }
}
