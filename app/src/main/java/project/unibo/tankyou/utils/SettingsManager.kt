package project.unibo.tankyou.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint
import project.unibo.tankyou.R
import project.unibo.tankyou.utils.Constants.App.LOG_TAG
import project.unibo.tankyou.utils.Constants.AppLanguage
import java.util.Locale

/**
 * Manages application settings such as language, map preferences, and user location display.
 *
 * Utilizes SharedPreferences for persistent storage of settings and provides
 * reactive state management through StateFlow and State for UI components.
 * All settings are persisted automatically when changed.
 */
object SettingsManager {
    private const val PREFS_NAME: String = "app_settings"

    private const val LANGUAGE_KEY: String = "selected_language"
    private const val SHOW_MY_LOCATION_KEY: String = "show_my_location"
    private const val MAP_TINT_KEY: String = "map_tint"

    private const val MAP_CENTER_LATITUDE_KEY: String = "map_center_latitude"
    private const val MAP_CENTER_LONGITUDE_KEY: String = "map_center_longitude"
    private const val MAP_ZOOM_LEVEL_KEY: String = "map_zoom_level"

    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context

    private val _mapTintFlow: MutableStateFlow<MapTint> = MutableStateFlow(MapTint.NONE)
    private val _showMyLocationOnMapFlow: MutableStateFlow<Boolean> = MutableStateFlow(true)

    // Language state management
    private val _currentLanguageFlow: MutableStateFlow<AppLanguage> =
        MutableStateFlow(AppLanguage.ITALIAN)
    private val _currentLanguage = mutableStateOf(AppLanguage.ITALIAN)

    /**
     * A State holding the current application language for Compose components.
     */
    val currentLanguage: State<AppLanguage> = _currentLanguage

    /**
     * A StateFlow indicating whether the user's location should be shown on the map.
     */
    val showMyLocationOnMapFlow: StateFlow<Boolean> = _showMyLocationOnMapFlow.asStateFlow()

    /**
     * A StateFlow holding the current map tint setting.
     */
    val mapTintFlow: StateFlow<MapTint> = _mapTintFlow.asStateFlow()

    /**
     * Enumeration for available map tint options.
     *
     * @param stringResId Resource ID for the localized display name
     * @param colorValue Color value to apply as tint overlay
     */
    enum class MapTint(@StringRes val stringResId: Int, val colorValue: Int) {
        NONE(R.string.map_tint_none, 0x00000000.toInt()),
        GRAY(R.string.map_tint_gray, 0xFFA9A9A9.toInt()),
        SEPIA(R.string.map_tint_sepia, 0xFF704214.toInt());

        /**
         * Gets the localized display name for this map tint.
         * This method requires SettingsManager to be initialized.
         *
         * @return The localized display name
         */
        fun getDisplayName(): String {
            return if (::appContext.isInitialized) {
                appContext.getResourceString(stringResId)
            } else {
                this.name
            }
        }
    }

    /**
     * Initializes the SettingsManager with the application context.
     *
     * This must be called before any other methods are used. It sets up
     * SharedPreferences and loads all existing settings from storage.
     *
     * @param context The application context
     */
    fun initialize(context: Context) {
        Log.d(LOG_TAG, "Initializing SettingsManager")

        try {
            appContext = context.applicationContext
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadAllSettings()
            Log.d(LOG_TAG, "SettingsManager initialized successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error initializing SettingsManager", e)
        }
    }

    /**
     * Loads all settings from SharedPreferences.
     *
     * If a setting is not found, it defaults to a predefined value.
     * This method is called during initialization and when resetting settings.
     */
    private fun loadAllSettings() {
        Log.d(LOG_TAG, "Loading all settings from SharedPreferences")
        try {
            // Load and set the application language
            val savedLanguage: String? = prefs.getString(LANGUAGE_KEY, AppLanguage.ITALIAN.code)
            val language: AppLanguage =
                AppLanguage.entries.find { it.code == savedLanguage } ?: AppLanguage.ITALIAN
            Log.d(LOG_TAG, "Loaded language setting: ${language.code}")

            // Update both StateFlow and State synchronously
            _currentLanguageFlow.value = language
            _currentLanguage.value = language

            // Load and set the 'show my location' preference
            val showLocation: Boolean = prefs.getBoolean(SHOW_MY_LOCATION_KEY, true)
            _showMyLocationOnMapFlow.value = showLocation
            Log.d(LOG_TAG, "Loaded show location setting: $showLocation")

            // Load and set the map tint preference
            val savedTintName: String? = prefs.getString(MAP_TINT_KEY, MapTint.NONE.name)
            val mapTint: MapTint = MapTint.entries.find { it.name == savedTintName } ?: MapTint.NONE
            _mapTintFlow.value = mapTint
            Log.d(LOG_TAG, "Loaded map tint setting: ${mapTint.name}")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error loading settings", e)
        }
    }

    /**
     * Sets the application language and optionally applies it to the context.
     *
     * @param language The language to set
     * @param context Optional context to apply the language change immediately
     */
    fun setLanguage(language: AppLanguage, context: Context? = null) {
        Log.d(LOG_TAG, "Setting language to: ${language.code}")
        try {
            // Update both StateFlow and State synchronously
            _currentLanguageFlow.value = language
            _currentLanguage.value = language

            prefs.edit { putString(LANGUAGE_KEY, language.code) }

            // Apply the language change to the application's locale if context is provided
            context?.let { applyLanguage(language) }
            Log.d(LOG_TAG, "Language set successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error setting language", e)
        }
    }

    /**
     * Applies the selected language to the application's locale.
     *
     * @param language The language to apply
     */
    private fun applyLanguage(language: AppLanguage) {
        Log.d(LOG_TAG, "Applying language: ${language.code}")
        try {
            val locale = Locale(language.code)
            Locale.setDefault(locale)
            // Note: For language changes to take full effect, the Activity might need to be recreated.
            Log.d(LOG_TAG, "Language applied successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error applying language", e)
        }
    }

    /**
     * Gets the currently selected application language.
     *
     * @return The current AppLanguage
     */
    fun getCurrentLanguage(): AppLanguage {
        val currentLang: AppLanguage = _currentLanguageFlow.value
        Log.d(LOG_TAG, "Getting current language: ${currentLang.code}")
        return currentLang
    }

    /**
     * Sets the preference for showing the user's location on the map.
     *
     * @param show True to show the location, false otherwise
     */
    fun setShowMyLocationOnMap(show: Boolean) {
        Log.d(LOG_TAG, "Setting show my location on map: $show")
        try {
            _showMyLocationOnMapFlow.value = show
            prefs.edit {
                putBoolean(SHOW_MY_LOCATION_KEY, show)
            }
            Log.d(LOG_TAG, "Show location setting saved successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error setting show location preference", e)
        }
    }

    /**
     * Sets the map tint preference.
     *
     * @param tint The map tint to apply
     */
    fun setMapTint(tint: MapTint) {
        Log.d(LOG_TAG, "Setting map tint: ${tint.name}")
        try {
            _mapTintFlow.value = tint
            prefs.edit {
                putString(MAP_TINT_KEY, tint.name)
            }
            Log.d(LOG_TAG, "Map tint setting saved successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error setting map tint", e)
        }
    }

    /**
     * Gets the currently selected map tint.
     *
     * @return The current MapTint
     */
    fun getCurrentMapTint(): MapTint {
        val currentTint: MapTint = _mapTintFlow.value
        Log.d(LOG_TAG, "Getting current map tint: ${currentTint.name}")
        return currentTint
    }

    /**
     * Saves the current map position and zoom level for restoration later.
     *
     * @param center The center point of the map
     * @param zoomLevel The current zoom level
     */
    fun saveMapPosition(center: GeoPoint, zoomLevel: Double) {
        Log.d(
            LOG_TAG,
            "Saving map position: lat=${center.latitude}, lon=${center.longitude}, zoom=$zoomLevel"
        )
        try {
            prefs.edit {
                putString(MAP_CENTER_LATITUDE_KEY, center.latitude.toString())
                putString(MAP_CENTER_LONGITUDE_KEY, center.longitude.toString())
                putString(MAP_ZOOM_LEVEL_KEY, zoomLevel.toString())
            }
            Log.d(LOG_TAG, "Map position saved successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error saving map position", e)
        }
    }

    /**
     * Retrieves the saved map center coordinates.
     *
     * If no saved center is found or if there's an error parsing the saved values,
     * it returns a default GeoPoint.
     *
     * @return The saved GeoPoint or a default one
     */
    fun getSavedMapCenter(): GeoPoint {
        Log.d(LOG_TAG, "Retrieving saved map center")
        try {
            val latStr: String? = prefs.getString(MAP_CENTER_LATITUDE_KEY, null)
            val lonStr: String? = prefs.getString(MAP_CENTER_LONGITUDE_KEY, null)

            return if (latStr != null && lonStr != null) {
                try {
                    val geoPoint = GeoPoint(latStr.toDouble(), lonStr.toDouble())
                    Log.d(
                        LOG_TAG,
                        "Retrieved saved map center: lat=${geoPoint.latitude}, lon=${geoPoint.longitude}"
                    )
                    geoPoint
                } catch (e: NumberFormatException) {
                    Log.w(LOG_TAG, "Error parsing saved map center coordinates, using default", e)
                    Constants.Map.DEFAULT_GEO_POINT
                }
            } else {
                Log.d(LOG_TAG, "No saved map center found, using default")
                Constants.Map.DEFAULT_GEO_POINT
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error retrieving saved map center", e)
            return Constants.Map.DEFAULT_GEO_POINT
        }
    }

    /**
     * Retrieves the saved map zoom level.
     *
     * If no saved zoom level is found or if there's an error parsing the saved value,
     * it returns a default zoom level.
     *
     * @return The saved zoom level or a default one
     */
    fun getSavedZoomLevel(): Double {
        Log.d(LOG_TAG, "Retrieving saved zoom level")
        try {
            val zoomStr: String? = prefs.getString(MAP_ZOOM_LEVEL_KEY, null)
            return if (zoomStr != null) {
                try {
                    val zoomLevel: Double = zoomStr.toDouble()
                    Log.d(LOG_TAG, "Retrieved saved zoom level: $zoomLevel")
                    zoomLevel
                } catch (e: NumberFormatException) {
                    Log.w(LOG_TAG, "Error parsing saved zoom level, using default", e)
                    Constants.Map.DEFAULT_ZOOM_LEVEL
                }
            } else {
                Log.d(LOG_TAG, "No saved zoom level found, using default")
                Constants.Map.DEFAULT_ZOOM_LEVEL
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error retrieving saved zoom level", e)
            return Constants.Map.DEFAULT_ZOOM_LEVEL
        }
    }

    /**
     * Resets all application settings to their default values.
     *
     * This clears all stored preferences and reloads the default settings.
     * Use with caution as this operation cannot be undone.
     */
    fun resetToDefaults() {
        Log.d(LOG_TAG, "Resetting all settings to defaults")
        try {
            prefs.edit {
                clear()
            }
            loadAllSettings()
            Log.d(LOG_TAG, "Settings reset to defaults successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error resetting settings to defaults", e)
        }
    }
}