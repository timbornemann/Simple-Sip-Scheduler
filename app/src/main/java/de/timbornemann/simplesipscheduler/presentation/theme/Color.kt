package de.timbornemann.simplesipscheduler.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)
val Red400 = Color(0xFFCF6679)

val Black = Color(0xFF000000)
val DarkGrey = Color(0xFF121212) // Slightly lighter for cards if needed, but aim for true black background
val BlueAccent = Color(0xFF4FC3F7)
val WaterBlue = Color(0xFF29B6F6)

internal val wearColorPalette: Colors = Colors(
    primary = WaterBlue,
    primaryVariant = Purple700,
    secondary = Teal200,
    secondaryVariant = Teal200,
    error = Red400,
    onPrimary = Black,
    onSecondary = Black,
    onError = Black,
    background = Black,
    onBackground = Color.White,
    surface = DarkGrey,
    onSurface = Color.White,
)
