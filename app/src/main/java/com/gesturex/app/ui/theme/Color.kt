package com.gesturex.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// GestureX Brand Colors
val PurplePrimary    = Color(0xFF7C4DFF)
val PurpleLight      = Color(0xFFB47CFF)
val PurpleDark       = Color(0xFF3F1DCB)
val CyanAccent       = Color(0xFF00E5FF)
val GreenSuccess     = Color(0xFF00E676)
val OrangeWarning    = Color(0xFFFF9100)
val RedError         = Color(0xFFFF1744)
val DarkBackground   = Color(0xFF0D0D0D)
val DarkSurface      = Color(0xFF1A1A2E)
val DarkSurface2     = Color(0xFF16213E)
val CardBackground   = Color(0xFF1E1E3A)

val DarkColorScheme = darkColorScheme(
    primary          = PurplePrimary,
    onPrimary        = Color.White,
    primaryContainer = PurpleDark,
    secondary        = CyanAccent,
    onSecondary      = Color.Black,
    background       = DarkBackground,
    surface          = DarkSurface,
    onBackground     = Color.White,
    onSurface        = Color.White,
    error            = RedError,
    surfaceVariant   = CardBackground,
)

val LightColorScheme = lightColorScheme(
    primary          = PurplePrimary,
    onPrimary        = Color.White,
    primaryContainer = PurpleLight,
    secondary        = PurpleDark,
    onSecondary      = Color.White,
    background       = Color(0xFFF5F5FF),
    surface          = Color.White,
    onBackground     = Color(0xFF1A1A2E),
    onSurface        = Color(0xFF1A1A2E),
    error            = RedError,
)
