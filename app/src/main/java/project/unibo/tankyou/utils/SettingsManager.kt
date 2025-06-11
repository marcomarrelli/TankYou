package project.unibo.tankyou.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import project.unibo.tankyou.utils.Constants.AppLanguage
import java.util.Locale

object SettingsManager {
    private const val PREFS_NAME = "app_settings"

    private const val LANGUAGE_KEY = "selected_language"
    private const val LOCATION_ENABLED_KEY = "location_enabled"
    private const val SHOW_MY_LOCATION_KEY = "show_my_location"
    private const val AUTO_CENTER_MAP_KEY = "auto_center_map"

    private lateinit var prefs: SharedPreferences

    private val _currentLanguageFlow = MutableStateFlow(AppLanguage.ITALIAN)

    private val _currentLanguage = mutableStateOf(AppLanguage.ITALIAN)
    val currentLanguage: State<AppLanguage> = _currentLanguage

    private val _locationEnabled = mutableStateOf(true)
    val locationEnabled: State<Boolean> = _locationEnabled

    private val _showMyLocationOnMapFlow = MutableStateFlow(true)
    val showMyLocationOnMapFlow: StateFlow<Boolean> = _showMyLocationOnMapFlow.asStateFlow()

    private val _autoCenterMap = mutableStateOf(false)

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadAllSettings()
    }

    private fun loadAllSettings() {
        val savedLanguage = prefs.getString(LANGUAGE_KEY, AppLanguage.ITALIAN.code)
        val language = AppLanguage.entries.find { it.code == savedLanguage } ?: AppLanguage.ITALIAN

        _currentLanguage.value = language
        _currentLanguageFlow.value = language

        _locationEnabled.value = prefs.getBoolean(LOCATION_ENABLED_KEY, true)

        _showMyLocationOnMapFlow.value = prefs.getBoolean(SHOW_MY_LOCATION_KEY, true)

        _autoCenterMap.value = prefs.getBoolean(AUTO_CENTER_MAP_KEY, false)
    }

    fun setLanguage(language: AppLanguage, context: Context? = null) {
        _currentLanguage.value = language
        _currentLanguageFlow.value = language
        prefs.edit { putString(LANGUAGE_KEY, language.code) }

        context?.let { applyLanguage(it, language) }
    }

    private fun applyLanguage(context: Context, language: AppLanguage) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
    }

    fun getCurrentLanguage(): AppLanguage = _currentLanguage.value

    fun setLocationEnabled(enabled: Boolean) {
        _locationEnabled.value = enabled
        prefs.edit { putBoolean(LOCATION_ENABLED_KEY, enabled) }
    }

    fun isLocationPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && _locationEnabled.value
    }

    fun shouldUseLocation(): Boolean = _locationEnabled.value

    fun setShowMyLocationOnMap(show: Boolean) {
        println("SettingsManager.setShowMyLocationOnMap called with: $show")
        _showMyLocationOnMapFlow.value = show
        prefs.edit { putBoolean(SHOW_MY_LOCATION_KEY, show) }
        println("SettingsManager.setShowMyLocationOnMap - StateFlow value now: ${_showMyLocationOnMapFlow.value}")
    }

    fun resetToDefaults() {
        prefs.edit { clear() }
        loadAllSettings()
    }
}