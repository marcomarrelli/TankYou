package project.unibo.tankyou.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint
import project.unibo.tankyou.utils.Constants.AppLanguage
import java.util.Locale

/**
 * Manages application settings such as language, map preferences, and user location display.
 * Utilizes SharedPreferences for persistent storage of settings.
 */
object SettingsManager {
    private const val PREFS_NAME = "app_settings"

    private const val LANGUAGE_KEY = "selected_language"
    private const val SHOW_MY_LOCATION_KEY = "show_my_location"
    private const val AUTO_CENTER_MAP_KEY = "auto_center_map"

    private const val MAP_CENTER_LATITUDE_KEY = "map_center_latitude"
    private const val MAP_CENTER_LONGITUDE_KEY = "map_center_longitude"
    private const val MAP_ZOOM_LEVEL_KEY = "map_zoom_level"

    private lateinit var prefs: SharedPreferences // SharedPreferences instance for storing settings

    private val _currentLanguageFlow = MutableStateFlow(AppLanguage.ITALIAN)

    private val _currentLanguage = mutableStateOf(AppLanguage.ITALIAN)

    /**
     * A [State] holding the current application language.
     */
    val currentLanguage: State<AppLanguage> = _currentLanguage

    private val _showMyLocationOnMapFlow = MutableStateFlow(true)

    /**
     * A [StateFlow] indicating whether the user's location should be shown on the map.
     */
    val showMyLocationOnMapFlow: StateFlow<Boolean> = _showMyLocationOnMapFlow.asStateFlow()

    private val _autoCenterMap = mutableStateOf(false)

    /**
     * Initializes the SettingsManager with the application context.
     * This must be called before any other methods are used.
     *
     * @param context The application context.
     */
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadAllSettings()
    }

    /**
     * Loads all settings from SharedPreferences.
     * If a setting is not found, it defaults to a predefined value.
     */
    private fun loadAllSettings() {
        // Load and set the application language
        val savedLanguage = prefs.getString(LANGUAGE_KEY, AppLanguage.ITALIAN.code)
        val language = AppLanguage.entries.find { it.code == savedLanguage } ?: AppLanguage.ITALIAN

        _currentLanguage.value = language
        _currentLanguageFlow.value = language

        // Load and set the 'show my location' preference
        _showMyLocationOnMapFlow.value = prefs.getBoolean(SHOW_MY_LOCATION_KEY, true)

        // Load and set the 'auto center map' preference
        _autoCenterMap.value = prefs.getBoolean(AUTO_CENTER_MAP_KEY, false)
    }

    fun setLanguage(language: AppLanguage, context: Context? = null) {
        _currentLanguage.value = language
        _currentLanguageFlow.value = language
        prefs.edit { putString(LANGUAGE_KEY, language.code) }

        // Apply the language change to the application's locale if context is provided
        context?.let { applyLanguage(it, language) }
    }

    /**
     * Applies the selected language to the application's locale.
     *
     * @param context The application context.
     * @param language The language to apply.
     */
    private fun applyLanguage(context: Context, language: AppLanguage) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        // Note: For language changes to take full effect, the Activity might need to be recreated.
    }

    /**
     * Gets the currently selected application language.
     *
     * @return The current [AppLanguage].
     */
    fun getCurrentLanguage(): AppLanguage = _currentLanguage.value

    /**
     * Sets the preference for showing the user's location on the map.
     *
     * @param show True to show the location, false otherwise.
     */
    fun setShowMyLocationOnMap(show: Boolean) {
        _showMyLocationOnMapFlow.value = show
        prefs.edit {
            putBoolean(SHOW_MY_LOCATION_KEY, show)
        }
        // TODO: Consider if autoCenterMap should be updated here based on 'show'
    }

    fun saveMapPosition(center: GeoPoint, zoomLevel: Double) {
        prefs.edit {
            putString(MAP_CENTER_LATITUDE_KEY, center.latitude.toString())
            putString(MAP_CENTER_LONGITUDE_KEY, center.longitude.toString())
            putString(MAP_ZOOM_LEVEL_KEY, zoomLevel.toString())
        }
    }

    /**
     * Retrieves the saved map center coordinates.
     * If no saved center is found or if there's an error parsing the saved values,
     * it returns a default [GeoPoint].
     * @return The saved [GeoPoint] or a default one.
     */
    fun getSavedMapCenter(): GeoPoint {
        val latStr = prefs.getString(MAP_CENTER_LATITUDE_KEY, null)
        val lonStr = prefs.getString(MAP_CENTER_LONGITUDE_KEY, null)

        return if (latStr != null && lonStr != null) {
            try {
                GeoPoint(latStr.toDouble(), lonStr.toDouble())
            } catch (e: NumberFormatException) {
                Constants.Map.DEFAULT_GEO_POINT // Fallback to default if parsing fails
            }
        } else {
            Constants.Map.DEFAULT_GEO_POINT // Fallback to default if no saved values
        }
    }

    /**
     * Retrieves the saved map zoom level.
     * If no saved zoom level is found or if there's an error parsing the saved value,
     * it returns a default zoom level.
     * @return The saved zoom level or a default one.
     */
    fun getSavedZoomLevel(): Double {
        val zoomStr = prefs.getString(MAP_ZOOM_LEVEL_KEY, null)
        return if (zoomStr != null) {
            try {
                zoomStr.toDouble()
            } catch (e: NumberFormatException) {
                Constants.Map.DEFAULT_ZOOM_LEVEL // Fallback to default if parsing fails
            }
        } else {
            Constants.Map.DEFAULT_ZOOM_LEVEL // Fallback to default if no saved value
        }
    }

    /**
     * Resets all application settings to their default values.
     * This clears all stored preferences and reloads the default settings.
     */
    fun resetToDefaults() {
        prefs.edit {
            clear()
        }
        loadAllSettings()
    }
}