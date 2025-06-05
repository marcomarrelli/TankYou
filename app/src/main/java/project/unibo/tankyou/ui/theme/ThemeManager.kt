package project.unibo.tankyou.ui.theme

import android.content.Context
import android.content.SharedPreferences

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

import androidx.core.content.edit

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

object ThemeManager {
    private const val PREFS_NAME = "theme_preferences"
    private const val THEME_KEY = "selected_theme"

    private lateinit var prefs: SharedPreferences
    private val _themeMode = mutableStateOf(ThemeMode.SYSTEM)
    val themeMode: State<ThemeMode> = _themeMode

    /** Color Cache */
    private var cachedColors: PaletteData? = null
    private var lastThemeMode: ThemeMode? = null
    private var isSystemDark: Boolean = false

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadTheme()
        updateSystemTheme(context)
        updateCachedColors()
    }

    private fun loadTheme() {
        val savedTheme = prefs.getString(THEME_KEY, ThemeMode.SYSTEM.name)
        _themeMode.value = ThemeMode.valueOf(savedTheme ?: ThemeMode.SYSTEM.name)
    }

    fun setTheme(themeMode: ThemeMode) {
        _themeMode.value = themeMode
        prefs.edit { putString(THEME_KEY, themeMode.name) }

        updateCachedColors()
    }

    fun toggleTheme() {
        val newTheme = when (_themeMode.value) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
        }
        setTheme(newTheme)
    }

    private fun updateSystemTheme(context: Context) {
        isSystemDark = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun updateCachedColors() {
        val currentTheme = _themeMode.value

        cachedColors = when (currentTheme) {
            ThemeMode.LIGHT -> LightTankYouColors
            ThemeMode.DARK -> DarkTankYouColors
            ThemeMode.SYSTEM -> if (isSystemDark) DarkTankYouColors else LightTankYouColors
        }

        lastThemeMode = currentTheme
    }

    fun getCurrentColors(): PaletteData {
        if (lastThemeMode != _themeMode.value) updateCachedColors()
        return cachedColors ?: LightTankYouColors
    }

    val palette get() = getCurrentColors()
}