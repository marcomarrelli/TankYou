package project.unibo.tankyou.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import project.unibo.tankyou.utils.Constants

/**
 * Enum representing the available theme modes in the application.
 *
 * This enum provides three distinct theme options that users can select from,
 * allowing for personalized visual experience or system integration.
 */
enum class ThemeMode {
    /** Light theme mode with bright colors */
    LIGHT,

    /** Dark theme mode with dark colors */
    DARK,

    /** System default theme mode that follows device settings */
    SYSTEM
}

/**
 * Extension function that converts a Jetpack Compose Color to an Android color integer.
 * This is useful for interoperability with Android APIs that require color integers.
 *
 * @return The Android color integer representation of this Color
 */
fun Color.toAndroidColor(): Int {
    return this.toArgb().also {
        Log.d(Constants.App.LOG_TAG, "Color converted to Android integer: $it")
    }
}

/**
 * Singleton object that manages the application's theme state and persistence.
 *
 * This manager handles switching between light, dark, and system default themes,
 * persists user preferences, and provides caching for performance optimization.
 * It should be initialized once during application startup.
 */
object ThemeManager {
    private const val PREFS_NAME: String = "theme_preferences"
    private const val THEME_KEY: String = "selected_theme"

    private lateinit var prefs: SharedPreferences
    private val _themeMode = mutableStateOf(ThemeMode.SYSTEM)

    /**
     * The current theme mode as a reactive Composable State.
     * UI components can observe this to automatically update when theme changes.
     *
     * @return State<ThemeMode> representing the current theme mode
     */
    val themeMode: State<ThemeMode> = _themeMode

    // Color Cache for performance optimization
    private var cachedColors: PaletteData? = null
    private var lastThemeMode: ThemeMode? = null
    private var isSystemDark: Boolean = false

    /**
     * Initializes the ThemeManager with application context.
     *
     * This method should be called once during application startup, typically
     * in the Application class. It loads saved preferences, determines system
     * theme state, and prepares the color cache.
     *
     * @param context The application context for accessing SharedPreferences
     */
    fun initialize(context: Context) {
        try {
            Log.d(Constants.App.LOG_TAG, "Initializing ThemeManager")

            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadTheme()
            updateSystemTheme(context)
            updateCachedColors()

            Log.i(
                Constants.App.LOG_TAG,
                "ThemeManager initialized successfully with theme: ${_themeMode.value}"
            )
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Failed to initialize ThemeManager", e)
            // Fallback to default state
            _themeMode.value = ThemeMode.SYSTEM
        }
    }

    /**
     * Loads the saved theme preference from SharedPreferences.
     *
     * If no preference is found or an error occurs, it defaults to ThemeMode.SYSTEM.
     * This ensures the app always has a valid theme mode set.
     */
    private fun loadTheme() {
        try {
            val savedTheme: String? = prefs.getString(THEME_KEY, ThemeMode.SYSTEM.name)
            _themeMode.value = ThemeMode.valueOf(savedTheme ?: ThemeMode.SYSTEM.name)
            Log.d(Constants.App.LOG_TAG, "Loaded theme from preferences: ${_themeMode.value}")
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Failed to load theme from preferences, using default", e)
            _themeMode.value = ThemeMode.SYSTEM
        }
    }

    /**
     * Sets the application theme to the specified ThemeMode.
     *
     * This method updates the internal state, persists the preference to storage,
     * and refreshes the cached colors to ensure consistency.
     *
     * @param themeMode The new theme mode to apply
     */
    fun setTheme(themeMode: ThemeMode) {
        try {
            Log.d(Constants.App.LOG_TAG, "Setting theme to: $themeMode")

            _themeMode.value = themeMode
            prefs.edit { putString(THEME_KEY, themeMode.name) }
            updateCachedColors()

            Log.i(Constants.App.LOG_TAG, "Theme successfully set to: $themeMode")
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Failed to set theme to: $themeMode", e)
        }
    }

    /**
     * Updates the isSystemDark flag based on the current system UI mode.
     *
     * This method checks the device's current dark mode setting and updates
     * the internal state accordingly for accurate theme determination.
     *
     * @param context The application context for accessing system configuration
     */
    private fun updateSystemTheme(context: Context) {
        try {
            isSystemDark = (context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
            Log.d(Constants.App.LOG_TAG, "System dark mode status updated: $isSystemDark")
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Failed to update system theme status", e)
            isSystemDark = false
        }
    }

    /**
     * Updates the cached colors based on the current theme mode and system state.
     *
     * This method ensures that the correct color palette is cached for the current
     * theme configuration, improving performance by avoiding repeated calculations.
     */
    private fun updateCachedColors() {
        try {
            val currentTheme: ThemeMode = _themeMode.value
            Log.d(Constants.App.LOG_TAG, "Updating cached colors for theme: $currentTheme")

            cachedColors = when (currentTheme) {
                ThemeMode.LIGHT -> LightTankYouColors
                ThemeMode.DARK -> DarkTankYouColors
                ThemeMode.SYSTEM -> if (isSystemDark) DarkTankYouColors else LightTankYouColors
            }

            lastThemeMode = currentTheme
            Log.d(Constants.App.LOG_TAG, "Cached colors updated successfully")
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Failed to update cached colors", e)
            cachedColors = LightTankYouColors // Fallback
        }
    }

    /**
     * Retrieves the current color palette with automatic cache management.
     *
     * If the theme mode has changed since the last retrieval, this method
     * automatically updates the cached colors. Falls back to light colors
     * if no cached colors are available.
     *
     * @return PaletteData representing the current theme's color palette
     */
    fun getCurrentColors(): PaletteData {
        try {
            // Re-calculate if theme has changed
            if (lastThemeMode != _themeMode.value) {
                Log.d(Constants.App.LOG_TAG, "Theme changed detected, updating colors")
                updateCachedColors()
            }
            return cachedColors ?: LightTankYouColors.also {
                Log.w(
                    Constants.App.LOG_TAG,
                    "No cached colors available, using light theme fallback"
                )
            }
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Failed to get current colors", e)
            return LightTankYouColors
        }
    }

    /**
     * Convenient property to access the current color palette.
     * This provides a simple way to get the current colors without calling a method.
     *
     * Usage example:
     * ```kotlin
     * @Composable
     * fun MyComponent() {
     *     val textColor: Color = ThemeManager.palette.text
     *
     *     Text(
     *         text = "Hello",
     *         color = textColor
     *     )
     * }
     * ```
     * @return PaletteData representing the current theme's color palette
     */
    val palette: PaletteData
        get() = getCurrentColors()
}