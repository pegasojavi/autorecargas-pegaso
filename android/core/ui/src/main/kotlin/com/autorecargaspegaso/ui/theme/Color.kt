package com.autorecargaspegaso.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta "Eco" (por defecto, CLAUDE.md sección de entorno visual definida
 * en el mockup de navegación): negro puro para OLED, acento bronce único
 * (color del escudo de Pegaso), sin gradientes. La paleta "Pulse"
 * (alternativa, azul-verde eléctrico) queda pendiente de implementar como
 * segundo `ColorScheme` seleccionable — no incluida en este primer scaffold.
 */
val PegasoBronze = Color(0xFFC9A227)
val PegasoBronzeDark = Color(0xFF8F7218)

val EcoBackground = Color(0xFF000000)
val EcoSurface = Color(0xFF121212)
val EcoSurfaceVariant = Color(0xFF1B1B1B)
val EcoOnBackground = Color(0xFFEDEDED)
val EcoOnSurfaceVariant = Color(0xFF8F8F8F)
val EcoOutline = Color(0xFF272727)
val EcoSuccess = Color(0xFF5CB88B)
val EcoDanger = Color(0xFFC2564B)
val EcoWarning = Color(0xFFC99A3D)

// Paleta clara de reserva (por si el sistema fuerza tema claro) — sigue el
// mismo acento bronce para mantener identidad de marca.
val LightBackground = Color(0xFFFFFBFE)
val LightSurface = Color(0xFFFFFBFE)
val LightOnBackground = Color(0xFF1C1B1F)
