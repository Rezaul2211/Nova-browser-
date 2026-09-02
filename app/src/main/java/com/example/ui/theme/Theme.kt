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

private val DarkColorScheme = darkColorScheme(
    primary = FrostedLavender,
    onPrimary = FrostedViolet,
    primaryContainer = FrostedContainerViolet,
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = FrostedIceBlue,
    onSecondary = Color(0xFF003258),
    secondaryContainer = Color(0xFF00497D),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = FrostedShieldGreen,
    onTertiary = Color(0xFF003915),
    background = FrostedDarkBackground,
    onBackground = FrostedTextPrimary,
    surface = FrostedDarkSurface,
    onSurface = FrostedTextPrimary,
    surfaceVariant = FrostedDarkSurfaceVariant,
    onSurfaceVariant = FrostedTextSecondary,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = FrostedLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = FrostedLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E4FF),
    onSecondaryContainer = Color(0xFF001D36),
    tertiary = FrostedLightTertiary,
    onTertiary = Color.White,
    background = FrostedLightBackground,
    onBackground = Color(0xFF191C1E),
    surface = FrostedLightSurface,
    onSurface = Color(0xFF191C1E),
    surfaceVariant = FrostedLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkThemeMode: String = "SYSTEM",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (darkThemeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
