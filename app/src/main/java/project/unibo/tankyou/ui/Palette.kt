package project.unibo.tankyou.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class PaletteData(
    val text: Color,
    val background: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val ok: Color,
    val warning: Color,
    val alert: Color//,
    //val onPrimary: Color,
    //val onSecondary: Color,
    //val onAccent: Color,
    //val onBackground: Color,
    //val onSurface: Color
)

val LightTankYouColors = PaletteData(
    text = LightColors.Text,
    background = LightColors.Background,
    primary = LightColors.Primary,
    secondary = LightColors.Secondary,
    accent = LightColors.Accent,
    ok = LightColors.OK,
    warning = LightColors.Warning,
    alert = LightColors.Alert//,
    //onPrimary = LightColors.OnPrimary,
    //onSecondary = LightColors.OnSecondary,
    //onAccent = LightColors.OnAccent,
    //onBackground = LightColors.OnBackground,
    //onSurface = LightColors.OnSurface
)

val DarkTankYouColors = PaletteData(
    text = DarkColors.Text,
    background = DarkColors.Background,
    primary = DarkColors.Primary,
    secondary = DarkColors.Secondary,
    accent = DarkColors.Accent,
    ok = DarkColors.OK,
    warning = DarkColors.Warning,
    alert = DarkColors.Alert//,
    //onPrimary = DarkColors.OnPrimary,
    //onSecondary = DarkColors.OnSecondary,
    //onAccent = DarkColors.OnAccent,
    //onBackground = DarkColors.OnBackground,
    //onSurface = DarkColors.OnSurface
)

val LocalTankYouColors = staticCompositionLocalOf { LightTankYouColors }

val Palette.colors: PaletteData
    @Composable
    @ReadOnlyComposable
    get() = LocalTankYouColors.current

object Palette