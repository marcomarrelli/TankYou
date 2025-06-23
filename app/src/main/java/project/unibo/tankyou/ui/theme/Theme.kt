package project.unibo.tankyou.ui.theme

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import project.unibo.tankyou.utils.Constants

/**
 * Dark color scheme for the TankYou theme.
 */
private val TankYouDarkColorScheme = darkColorScheme(
    primary = DarkColors.Primary,
    secondary = DarkColors.Secondary,
    tertiary = DarkColors.Accent,
    background = DarkColors.Background,
    surface = DarkColors.Background
)

/**
 * Light color scheme for the TankYou theme.
 */
private val TankYouLightColorScheme = lightColorScheme(
    primary = LightColors.Primary,
    secondary = LightColors.Secondary,
    tertiary = LightColors.Accent,
    background = LightColors.Background,
    surface = LightColors.Background
)

/**
 * Composable function that applies the TankYou theme to its content.
 *
 * @param themeMode The desired theme mode (Light, Dark, or System). Defaults to System
 * @param dynamicColor Whether to use dynamic colors (Material You) if available. Defaults to false
 * @param content The content to be themed
 */
@Composable
fun TankYouTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    Log.d(
        Constants.App.LOG_TAG,
        "TankYouTheme called with themeMode: $themeMode, dynamicColor: $dynamicColor"
    )

    // Determine if dark theme should be applied based on the themeMode
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    Log.d(Constants.App.LOG_TAG, "Dark theme determined: $darkTheme")

    // Determine the color scheme to use
    val colorScheme = when {
        // If dynamic color is enabled and the Android version supports it (S+),
        // use dynamic light or dark color scheme based on the darkTheme value.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            Log.d(Constants.App.LOG_TAG, "Using dynamic color scheme")
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Otherwise, use the predefined TankYouDarkColorScheme or TankYouLightColorScheme
        // based on the darkTheme value.
        darkTheme -> {
            Log.d(Constants.App.LOG_TAG, "Using TankYouDarkColorScheme")
            TankYouDarkColorScheme
        }

        else -> {
            Log.d(Constants.App.LOG_TAG, "Using TankYouLightColorScheme")
            TankYouLightColorScheme
        }
    }

    val tankYouColors: PaletteData = if (darkTheme) DarkTankYouColors else LightTankYouColors
    Log.d(Constants.App.LOG_TAG, "TankYou colors selected: ${if (darkTheme) "Dark" else "Light"}")

    val view = LocalView.current
    if (!view.isInEditMode) {
        // Side effect to update the system status bar appearance
        SideEffect {
            try {
                val window: Window = (view.context as Activity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                    !darkTheme
                Log.d(Constants.App.LOG_TAG, "System status bar appearance updated")
            } catch (e: Exception) {
                Log.e(Constants.App.LOG_TAG, "Failed to update system status bar appearance", e)
            }
        }
    }

    CompositionLocalProvider(LocalTankYouColors provides tankYouColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content
        )
    }
}