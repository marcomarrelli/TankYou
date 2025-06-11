package project.unibo.tankyou.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import project.unibo.tankyou.utils.Constants.AppLanguage
import java.util.Locale

object SettingsManager {
    private const val PREFS_NAME = "app_settings"

    private const val LANGUAGE_KEY = "selected_language"
    private const val LOCATION_ENABLED_KEY = "location_enabled"
    private const val SHOW_MY_LOCATION_KEY = "show_my_location"
    private const val SHOW_GAS_PRICES_KEY = "show_gas_prices"
    private const val AUTO_CENTER_MAP_KEY = "auto_center_map"
    private const val ENABLE_HAPTIC_FEEDBACK_KEY = "enable_haptic_feedback"
    private const val SHOW_TRAFFIC_KEY = "show_traffic"

    private lateinit var prefs: SharedPreferences

    private val _currentLanguage = mutableStateOf(AppLanguage.ITALIAN)
    val currentLanguage: State<AppLanguage> = _currentLanguage

    private val _locationEnabled = mutableStateOf(true)
    val locationEnabled: State<Boolean> = _locationEnabled

    private val _showMyLocationOnMap = mutableStateOf(true)
    val showMyLocationOnMap: State<Boolean> = _showMyLocationOnMap

    private val _showGasPrices = mutableStateOf(true)
    val showGasPrices: State<Boolean> = _showGasPrices

    private val _autoCenterMap = mutableStateOf(false)
    val autoCenterMap: State<Boolean> = _autoCenterMap

    private val _hapticFeedbackEnabled = mutableStateOf(true)
    val hapticFeedbackEnabled: State<Boolean> = _hapticFeedbackEnabled

    private val _showTraffic = mutableStateOf(false)
    val showTraffic: State<Boolean> = _showTraffic

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadAllSettings()
    }

    private fun loadAllSettings() {
        val savedLanguage = prefs.getString(LANGUAGE_KEY, AppLanguage.ITALIAN.code)
        _currentLanguage.value =
            AppLanguage.entries.find { it.code == savedLanguage } ?: AppLanguage.ITALIAN

        _locationEnabled.value = prefs.getBoolean(LOCATION_ENABLED_KEY, true)

        _showMyLocationOnMap.value = prefs.getBoolean(SHOW_MY_LOCATION_KEY, true)

        _showGasPrices.value = prefs.getBoolean(SHOW_GAS_PRICES_KEY, true)
        _autoCenterMap.value = prefs.getBoolean(AUTO_CENTER_MAP_KEY, false)

        _hapticFeedbackEnabled.value = prefs.getBoolean(ENABLE_HAPTIC_FEEDBACK_KEY, true)
        _showTraffic.value = prefs.getBoolean(SHOW_TRAFFIC_KEY, false)
    }

    fun setLanguage(language: AppLanguage, context: Context) {
        _currentLanguage.value = language
        prefs.edit { putString(LANGUAGE_KEY, language.code) }

        applyLanguage(context, language)
    }

    private fun applyLanguage(context: Context, language: AppLanguage) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)
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
        _showMyLocationOnMap.value = show
        prefs.edit { putBoolean(SHOW_MY_LOCATION_KEY, show) }
    }

    fun setShowGasPrices(show: Boolean) {
        _showGasPrices.value = show
        prefs.edit { putBoolean(SHOW_GAS_PRICES_KEY, show) }
    }

    fun setAutoCenterMap(autoCenter: Boolean) {
        _autoCenterMap.value = autoCenter
        prefs.edit { putBoolean(AUTO_CENTER_MAP_KEY, autoCenter) }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        _hapticFeedbackEnabled.value = enabled
        prefs.edit { putBoolean(ENABLE_HAPTIC_FEEDBACK_KEY, enabled) }
    }

    fun setShowTraffic(show: Boolean) {
        _showTraffic.value = show
        prefs.edit { putBoolean(SHOW_TRAFFIC_KEY, show) }
    }

    fun resetToDefaults() {
        prefs.edit { clear() }
        loadAllSettings()
    }

    fun exportSettings(): Map<String, Any?> {
        return mapOf(
            "language" to _currentLanguage.value.code,
            "locationEnabled" to _locationEnabled.value,
            "showMyLocationOnMap" to _showMyLocationOnMap.value,
            "showGasPrices" to _showGasPrices.value,
            "autoCenterMap" to _autoCenterMap.value,
            "hapticFeedbackEnabled" to _hapticFeedbackEnabled.value,
            "showTraffic" to _showTraffic.value
        )
    }
}