package project.unibo.tankyou.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class PaletteData(
    val title: Color,
    val text: Color,
    val background: Color,
    val border: Color,

    val disabledText: Color,
    val disabledBackground: Color,
    val disabledBorder: Color,

    val primary: Color,
    val secondary: Color,
    val accent: Color,

    val ok: Color,
    val warning: Color,
    val alert: Color,

    val white: Color,
    val black: Color
)

val LightTankYouColors = PaletteData(
    title = LightColors.Title,
    text = LightColors.Text,
    background = LightColors.Background,
    border = LightColors.Border,

    disabledText = LightColors.DisabledText,
    disabledBackground = LightColors.DisabledBackground,
    disabledBorder = LightColors.DisabledBorder,

    primary = LightColors.Primary,
    secondary = LightColors.Secondary,
    accent = LightColors.Accent,

    ok = LightColors.OK,
    warning = LightColors.Warning,
    alert = LightColors.Alert,

    white = LightColors.White,
    black = LightColors.Black
)

val DarkTankYouColors = PaletteData(
    title = DarkColors.Title,
    text = DarkColors.Text,
    background = DarkColors.Background,
    border = DarkColors.Border,

    disabledText = DarkColors.DisabledText,
    disabledBackground = DarkColors.DisabledBackground,
    disabledBorder = DarkColors.DisabledBorder,

    primary = DarkColors.Primary,
    secondary = DarkColors.Secondary,
    accent = DarkColors.Accent,

    ok = DarkColors.OK,
    warning = DarkColors.Warning,
    alert = DarkColors.Alert,

    white = DarkColors.White,
    black = DarkColors.Black
)

val LocalTankYouColors = staticCompositionLocalOf { LightTankYouColors }