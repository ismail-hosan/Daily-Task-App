package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val NaturalLightColorScheme = lightColorScheme(
  primary = NaturalPrimary,
  onPrimary = Color.White,
  primaryContainer = NaturalPrimaryContainer,
  onPrimaryContainer = NaturalText,
  secondary = NaturalTextSecondary,
  onSecondary = Color.White,
  background = NaturalBg,
  onBackground = NaturalText,
  surface = Color.White,
  onSurface = NaturalText,
  outline = NaturalBorder,
  surfaceVariant = NaturalContainer,
  onSurfaceVariant = NaturalTextSecondary
)

private val DarkColorScheme =
  darkColorScheme(
    primary = NaturalPrimaryContainer,
    onPrimary = NaturalText,
    primaryContainer = NaturalPrimary,
    onPrimaryContainer = Color.White,
    secondary = NaturalTextSecondary,
    onSecondary = Color.White,
    background = Color(0xFF1C1E1B),
    onBackground = Color(0xFFE2E3DE),
    surface = Color(0xFF242623),
    onSurface = Color(0xFFE2E3DE)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Set to false by default to preserve the beautiful custom Natural Tones theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> NaturalLightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
