package com.example.pasajeapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Teal300,
    onPrimary = Navy900,
    primaryContainer = Navy800,
    onPrimaryContainer = Navy100,
    secondary = Teal300,
    onSecondary = Navy900,
    secondaryContainer = DarkTealContainer,
    onSecondaryContainer = Teal100,
    tertiary = Amber300,
    onTertiary = Neutral950,
    tertiaryContainer = DarkAmberContainer,
    onTertiaryContainer = Amber100,
    background = Neutral950,
    onBackground = Neutral100,
    surface = Neutral900,
    onSurface = Neutral100,
    surfaceVariant = Neutral800,
    onSurfaceVariant = Neutral300,
    outline = Neutral500,
    outlineVariant = Neutral700,
    error = ErrorDark,
    onError = ErrorContainerDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorContainerLight
)

private val LightColorScheme = lightColorScheme(
    primary = Navy900,
    onPrimary = White,
    primaryContainer = Navy100,
    onPrimaryContainer = Navy900,
    secondary = Teal700,
    onSecondary = White,
    secondaryContainer = Teal100,
    onSecondaryContainer = Navy900,
    tertiary = Amber700,
    onTertiary = White,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber700,
    background = Neutral50,
    onBackground = Navy900,
    surface = White,
    onSurface = Navy900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral600,
    outline = Neutral500,
    outlineVariant = Neutral200,
    error = ErrorLight,
    onError = White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorLight
)

@Composable
fun PasajeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PasajeShapes,
        content = content
    )
}
