package com.autorecargaspegaso

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
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
            // Provisión explícita del NavigationEventDispatcherOwner — bug
            // real detectado en dispositivo: `NavDisplay` (navigation3-ui
            // 1.0.1) exige `LocalNavigationEventDispatcherOwner.current` no
            // nulo y crashea con IllegalStateException si falta. Esa local
            // solo se resuelve sola si `ComponentActivity` corre
            // `initializeViewTreeOwners()` (lo que fija
            // `ViewTreeNavigationEventDispatcherOwner` en el decor view), y
            // eso solo ocurre dentro de `ComponentActivity.setContentView()`
            // — pero `AppCompatActivity.setContentView()` (appcompat 1.7.0)
            // delega enteramente en `AppCompatDelegate.setContentView()` sin
            // llamar a `super`, así que ese cableado nunca se ejecuta.
            // `activity-compose` 1.13.0 compensa manualmente esto mismo para
            // Lifecycle/ViewModelStore/SavedStateRegistry dentro de
            // `setContent()`, pero no para el dispatcher de navegación, así
            // que hay que proveerlo a mano aquí. `MainActivity` ya es un
            // `NavigationEventDispatcherOwner` por herencia de
            // `ComponentActivity` (androidx.activity 1.13.0), así que basta
            // con exponerla explícitamente vía composition local — no hace
            // falta (ni se debe) volver a `ComponentActivity` para esto, eso
            // rompería el selector de idioma (ver comentario de la clase).
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides this@MainActivity) {
                AutoRecargasPegasoTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        MainNavigation()
                    }
                }
            }
        }
    }
}
