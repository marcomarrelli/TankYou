package project.unibo.tankyou.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
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

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadTheme()
    }

    private fun loadTheme() {
        val savedTheme = prefs.getString(THEME_KEY, ThemeMode.SYSTEM.name)
        _themeMode.value = ThemeMode.valueOf(savedTheme ?: ThemeMode.SYSTEM.name)
    }

    fun setTheme(themeMode: ThemeMode) {
        _themeMode.value = themeMode
        prefs.edit { putString(THEME_KEY, themeMode.name) }
    }

    fun toggleTheme() {
        val newTheme = when (_themeMode.value) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
        }
        setTheme(newTheme)
    }
}