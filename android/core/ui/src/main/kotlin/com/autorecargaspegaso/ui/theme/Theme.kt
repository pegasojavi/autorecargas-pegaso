package com.autorecargaspegaso.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Tema "Eco" (por defecto): negro puro, acento bronce único, sin
 * gradientes. Es el único tema implementado en este primer scaffold — el
 * tema alternativo "Pulse" (azul-verde eléctrico, ver el mockup de
 * navegación) queda como trabajo futuro de `builder-android`.
 */
private val EcoColorScheme = darkColorScheme(
    primary = PegasoBronze,
    onPrimary = EcoBackground,
    secondary = PegasoBronzeDark,
    background = EcoBackground,
    onBackground = EcoOnBackground,
    surface = EcoSurface,
    onSurface = EcoOnBackground,
    surfaceVariant = EcoSurfaceVariant,
    onSurfaceVariant = EcoOnSurfaceVariant,
    outline = EcoOutline,
    error = EcoDanger,
)

private val FallbackLightColorScheme = lightColorScheme(
    primary = PegasoBronze,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
)

@Composable
fun AutoRecargasPegasoTheme(
    useLightFallback: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useLightFallback) FallbackLightColorScheme else EcoColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
