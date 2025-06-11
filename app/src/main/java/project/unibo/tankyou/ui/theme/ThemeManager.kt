package project.unibo.tankyou.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import project.unibo.tankyou.ui.theme.ThemeManager.cachedColors
import project.unibo.tankyou.ui.theme.ThemeManager.isSystemDark
import project.unibo.tankyou.ui.theme.ThemeManager.themeMode

/**
 * Enum representing the available theme modes.
 */
enum class ThemeMode {
    /** Light theme mode. */
    LIGHT,

    /** Dark theme mode. */
    DARK,

    /** System default theme mode. */
    SYSTEM
}

/**
 * Converts a Jetpack Compose [Color] to an Android color integer.
 * @return The Android color integer.
 */
fun Color.toAndroidColor(): Int = this.toArgb()

/**
 * Manages the application's theme, allowing users to switch between light, dark, and system default modes.
 * It also handles caching of color palettes to optimize performance.
 */
object ThemeManager {
    private const val PREFS_NAME = "theme_preferences"
    private const val THEME_KEY = "selected_theme"

    private lateinit var prefs: SharedPreferences
    private val _themeMode = mutableStateOf(ThemeMode.SYSTEM)

    /**
     * The current theme mode as a Composable [State].
     */
    val themeMode: State<ThemeMode> = _themeMode

    // Color Cache for performance
    private var cachedColors: PaletteData? = null
    private var lastThemeMode: ThemeMode? = null
    private var isSystemDark: Boolean = false

    /**
     * Initializes the ThemeManager. This should be called once, typically in the Application class.
     * It loads the saved theme preference, determines the system theme, and updates the cached colors.
     *
     * @param context The application context.
     */
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadTheme()
        updateSystemTheme(context)
        updateCachedColors()
    }

    /**
     * Loads the saved theme preference from SharedPreferences.
     * If no preference is found, it defaults to [ThemeMode.SYSTEM].
     */
    private fun loadTheme() {
        val savedTheme = prefs.getString(THEME_KEY, ThemeMode.SYSTEM.name)
        _themeMode.value = ThemeMode.valueOf(savedTheme ?: ThemeMode.SYSTEM.name)
    }

    /**
     * Sets the application theme to the specified [ThemeMode].
     * This updates the internal state, saves the preference, and refreshes the cached colors.
     * @param themeMode The new theme mode to apply.
     */
    fun setTheme(themeMode: ThemeMode) {
        _themeMode.value = themeMode
        prefs.edit { putString(THEME_KEY, themeMode.name) }

        updateCachedColors()
    }

    /**
     * Toggles the theme in a cycle: LIGHT -> DARK -> SYSTEM -> LIGHT.
     * This provides a simple way for users to switch themes.
     */
    fun toggleTheme() {
        val newTheme = when (_themeMode.value) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
        }
        setTheme(newTheme)
    }

    /**
     * Updates the [isSystemDark] flag based on the current system UI mode.
     * @param context The application context.
     */
    private fun updateSystemTheme(context: Context) {
        isSystemDark = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Updates the [cachedColors] based on the current [themeMode] and [isSystemDark] state.
     * This ensures that the correct color palette is used.
     */
    private fun updateCachedColors() {
        val currentTheme = _themeMode.value

        cachedColors = when (currentTheme) {
            ThemeMode.LIGHT -> LightTankYouColors
            ThemeMode.DARK -> DarkTankYouColors
            ThemeMode.SYSTEM -> if (isSystemDark) DarkTankYouColors else LightTankYouColors
        }

        lastThemeMode = currentTheme
    }

    /**
     * Retrieves the current color palette.
     * If the theme mode has changed since the last retrieval, it updates the cached colors.
     * Defaults to [LightTankYouColors] if no cached colors are available.
     * @return The current [PaletteData].
     */
    fun getCurrentColors(): PaletteData {
        // Re-calculate if theme has changed
        if (lastThemeMode != _themeMode.value) updateCachedColors()
        return cachedColors ?: LightTankYouColors
    }

    /**
     * A convenient property to access the current color palette.
     */
    val palette get() = getCurrentColors()
}