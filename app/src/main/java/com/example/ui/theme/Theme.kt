package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryCyan,
    secondary = PrimaryPurple,
    tertiary = AccentPink,
    background = DeepBackground,
    surface = CardBackground,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
  )

private val LightColorScheme =
  darkColorScheme(
    primary = PrimaryCyan,
    secondary = PrimaryPurple,
    tertiary = AccentPink,
    background = DeepBackground,
    surface = CardBackground,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for AMOLED Black look
  dynamicColor: Boolean = false, // Disable dynamic colors to keep holographic aesthetics intact
  content: @Composable () -> Unit,
) {
  // Always use our custom DarkColorScheme for that AMOLED pitch-black premium feel
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
