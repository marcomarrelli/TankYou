package project.unibo.tankyou.ui.theme

import android.util.Log
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import project.unibo.tankyou.utils.Constants

/**
 * Data class representing the complete color palette for the application.
 *
 * This class holds all color definitions used across different UI elements and states,
 * providing a centralized and type-safe way to access theme colors. Each property
 * represents a specific use case in the application's visual design.
 *
 * @param title The color for titles and primary headings
 * @param text The primary color for body text and content
 * @param background The main background color for screens and containers
 * @param border The color for borders, dividers, and outlines
 *
 * @param disabledText The color for text in a disabled or inactive state
 * @param disabledBackground The background color for elements in a disabled state
 * @param disabledBorder The border color for elements in a disabled state
 *
 * @param primary The primary brand color for main actions and emphasis
 * @param secondary The secondary brand color for complementary elements
 * @param accent The accent color for highlighting and drawing attention
 *
 * @param ok The color indicating success, positive feedback, or OK status
 * @param warning The color indicating warnings or cautionary states
 * @param alert The color indicating alerts, errors, or critical states
 *
 * @param white A basic white color for high contrast elements
 * @param black A basic black color for high contrast elements
 */
data class PaletteData(
    // Colors for basic UI elements
    val title: Color,
    val text: Color,
    val background: Color,
    val border: Color,

    // Colors for disabled states
    val disabledText: Color,
    val disabledBackground: Color,
    val disabledBorder: Color,

    // Primary, secondary, and accent colors for branding and emphasis
    val primary: Color,
    val secondary: Color,
    val accent: Color,

    // Semantic colors for conveying status and meaning
    val ok: Color,
    val warning: Color,
    val alert: Color,

    // Basic utility colors for high contrast scenarios
    val white: Color,
    val black: Color
)

/**
 * Predefined color palette for the light theme of the TankYou application.
 *
 * This palette maps colors from the LightColors object to the structured
 * PaletteData format, providing a complete set of colors optimized for
 * light mode viewing conditions.
 *
 * Includes colors for:
 * - Basic UI elements (title, text, background, border)
 * - Disabled states for accessibility
 * - Brand identity (primary, secondary, accent)
 * - Status indication (ok, warning, alert)
 * - Utility colors (white, black)
 *
 * @return PaletteData configured for light theme
 */
val LightTankYouColors: PaletteData = PaletteData(
    // Basic UI colors
    title = LightColors.Title,
    text = LightColors.Text,
    background = LightColors.Background,
    border = LightColors.Border,

    // Disabled state colors optimized for light theme accessibility
    disabledText = LightColors.DisabledText,
    disabledBackground = LightColors.DisabledBackground,
    disabledBorder = LightColors.DisabledBorder,

    primary = LightColors.Primary,
    secondary = LightColors.Secondary,
    accent = LightColors.Accent,

    // Status colors for user feedback and cluster fullness indication
    ok = LightColors.OK,
    warning = LightColors.Warning,
    alert = LightColors.Alert,

    // Basic utility colors for high contrast scenarios
    white = LightColors.White,
    black = LightColors.Black
)

/**
 * Predefined color palette for the dark theme of the TankYou application.
 *
 * This palette maps colors from the DarkColors object to the structured
 * PaletteData format, providing a complete set of colors optimized for
 * dark mode viewing conditions and reduced eye strain.
 *
 * Includes colors for:
 * - Basic UI elements (title, text, background, border)
 * - Disabled states for accessibility
 * - Brand identity (primary, secondary, accent)
 * - Status indication (ok, warning, alert)
 * - Utility colors (white, black)
 *
 * @return PaletteData configured for dark theme
 */
val DarkTankYouColors: PaletteData = PaletteData(
    // Basic UI colors
    title = DarkColors.Title,
    text = DarkColors.Text,
    background = DarkColors.Background,
    border = DarkColors.Border,

    // Disabled state colors optimized for dark theme accessibility
    disabledText = DarkColors.DisabledText,
    disabledBackground = DarkColors.DisabledBackground,
    disabledBorder = DarkColors.DisabledBorder,

    primary = DarkColors.Primary,
    secondary = DarkColors.Secondary,
    accent = DarkColors.Accent,

    // Status colors for user feedback and cluster fullness indication
    ok = DarkColors.OK,
    warning = DarkColors.Warning,
    alert = DarkColors.Alert,

    // Basic utility colors for high contrast scenarios
    white = DarkColors.White,
    black = DarkColors.Black
)

/**
 * CompositionLocal provider for accessing the current theme's PaletteData.
 *
 * This CompositionLocal allows Composable functions throughout the app to easily
 * access the appropriate color palette based on the currently active theme
 * (light or dark). It provides a reactive way to access colors that automatically
 * updates when the theme changes.
 *
 *
 * @return CompositionLocal<PaletteData> with default value of LightTankYouColors
 */
val LocalTankYouColors: ProvidableCompositionLocal<PaletteData> = staticCompositionLocalOf {
    Log.d(Constants.App.LOG_TAG, "LocalTankYouColors CompositionLocal created")
    LightTankYouColors
}