package project.unibo.tankyou.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Data class representing the color palette for the application.
 * It holds various color definitions used across different UI elements and states.
 *
 * @property title The color for titles.
 * @property text The primary color for text.
 * @property background The main background color.
 * @property border The color for borders.
 * @property disabledText The color for text in a disabled state.
 * @property disabledBackground The background color for elements in a disabled state.
 * @property disabledBorder The border color for elements in a disabled state.
 * @property primary The primary brand color.
 * @property secondary The secondary brand color.
 * @property accent The accent color, often used for highlighting.
 * @property ok The color indicating a success or "OK" state.
 * @property warning The color indicating a warning state.
 * @property alert The color indicating an alert or error state.
 * @property white A basic white color.
 * @property black A basic black color.
 */
data class PaletteData(
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

    // Semantic colors for conveying status
    val ok: Color,
    val warning: Color,
    val alert: Color,

    // Basic utility colors
    val white: Color,
    val black: Color
)

/**
 * Defines the color palette for the light theme of the TankYou application.
 * These colors are mapped from the generic `LightColors` definition.
 *
 * Includes colors for:
 * - Basic UI elements (title, text, background, border)
 * - Disabled states (disabledText, disabledBackground, disabledBorder)
 * - Branding (primary, secondary, accent)
 * - Cluster Fullness states (ok, warning, alert)
 * - Utility colors (white, black)
 */
val LightTankYouColors = PaletteData(
    title = LightColors.Title,
    text = LightColors.Text,
    background = LightColors.Background,
    border = LightColors.Border,

    // Disabled state colors for the light theme
    disabledText = LightColors.DisabledText,
    disabledBackground = LightColors.DisabledBackground,
    disabledBorder = LightColors.DisabledBorder,

    primary = LightColors.Primary,
    secondary = LightColors.Secondary,
    accent = LightColors.Accent,

    // Cluster Fullness colors for the light theme
    ok = LightColors.OK,
    warning = LightColors.Warning,
    alert = LightColors.Alert,

    white = LightColors.White,
    black = LightColors.Black
)

/**
 * Defines the color palette for the dark theme of the TankYou application.
 * These colors are mapped from the generic `DarkColors` definition.
 *
 * Includes colors for:
 * - Basic UI elements (title, text, background, border)
 * - Disabled states (disabledText, disabledBackground, disabledBorder)
 * - Branding (primary, secondary, accent)
 * - Cluster Fullness states (ok, warning, alert)
 * - Utility colors (white, black)
 */
val DarkTankYouColors = PaletteData(
    title = DarkColors.Title,
    text = DarkColors.Text,
    background = DarkColors.Background,
    border = DarkColors.Border,


    // Disabled state colors for the dark theme
    disabledText = DarkColors.DisabledText,
    disabledBackground = DarkColors.DisabledBackground,
    disabledBorder = DarkColors.DisabledBorder,

    primary = DarkColors.Primary,
    secondary = DarkColors.Secondary,
    accent = DarkColors.Accent,

    // Cluster Fullness colors for the dark theme
    ok = DarkColors.OK,
    warning = DarkColors.Warning,
    alert = DarkColors.Alert,

    white = DarkColors.White,
    black = DarkColors.Black
)

/**
 * A CompositionLocal provider for accessing the current theme's `PaletteData`.
 * This allows Composable functions to easily obtain the appropriate colors based on whether
 * the light or dark theme is active. Defaults to `LightTankYouColors`.
 */
val LocalTankYouColors = staticCompositionLocalOf { LightTankYouColors }