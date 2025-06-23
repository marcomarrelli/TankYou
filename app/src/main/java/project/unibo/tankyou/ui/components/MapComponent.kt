package project.unibo.tankyou.ui.components

import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.location.Location
import android.util.Log
import android.view.MotionEvent
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.repositories.SearchFilters
import project.unibo.tankyou.utils.Constants
import project.unibo.tankyou.utils.Debouncer
import project.unibo.tankyou.utils.SettingsManager
import kotlin.math.abs

/**
 * A comprehensive map component that manages OpenStreetMap display with gas station markers, clustering, and location tracking.
 *
 * Extends [MapListener] to handle map events & [IMyLocationConsumer] to handle location updates.
 *
 * This component handles:
 * - Map initialization and configuration
 * - Gas station marker clustering and display
 * - Location tracking and follow mode
 * - Map tinting and visual customization
 * - Search functionality with filters
 * - Touch interaction detection for smart follow mode management
 *
 * @param context The AppCompatActivity context for lifecycle management
 * @param mapContainer The RelativeLayout container where the map will be added
 * @param onMapClick Callback triggered when the map is clicked
 * @param onGasStationClick Callback triggered when a gas station marker is clicked
 * @param onFollowModeChanged Callback triggered when follow mode state changes
 * @param onLocationOverlayAvailabilityChanged Callback triggered when location overlay availability changes
 * @param onSearchStateChanged Callback triggered when search state changes
 */
class MapComponent(
    private val context: AppCompatActivity,
    private val mapContainer: RelativeLayout,
    private val onMapClick: () -> Unit = {},
    private val onGasStationClick: (GasStation) -> Unit = {},
    private val onFollowModeChanged: (Boolean) -> Unit = {},
    private val onLocationOverlayAvailabilityChanged: (Boolean) -> Unit = {},
    private val onSearchStateChanged: (Boolean) -> Unit = {}
) : MapListener, IMyLocationConsumer {
    private lateinit var map: MapView

    private var locationOverlay: MyLocationNewOverlay? = null
    private var locationProvider: GpsMyLocationProvider? = null
    private var clusterer: GasStationCluster? = null
    private var mapInitialized: Boolean = false
    private var currentZoomLevel: Double = Constants.Map.DEFAULT_ZOOM_LEVEL
    private var lastLoadedBounds: BoundingBox? = null
    private val debounceHelper: Debouncer = Debouncer()

    private var isFollowingLocation: Boolean = false
    private var lastLocationUpdate: Long = 0

    // Touch interaction tracking for smart follow mode management
    private var hasUserTouchInteraction: Boolean = false
    private var lastUserTouchTime: Long = 0L

    init {
        Log.d(Constants.App.LOG_TAG, "Initializing MapComponent")
        setupMapConfiguration()
        setupSettingsObserver()
    }

    /**
     * Sets up observers for settings changes to react to user preferences.
     */
    private fun setupSettingsObserver() {
        Log.d(Constants.App.LOG_TAG, "Setting up settings observers")

        // Observer for location display setting changes
        context.lifecycleScope.launch {
            SettingsManager.showMyLocationOnMapFlow.collect { showLocation: Boolean ->
                Log.d(Constants.App.LOG_TAG, "Location display setting changed: $showLocation")
                if (mapInitialized) {
                    updateLocationOverlay()
                }
            }
        }

        // Observer for map tint setting changes
        context.lifecycleScope.launch {
            SettingsManager.mapTintFlow.collect { mapTint: SettingsManager.MapTint ->
                Log.d(Constants.App.LOG_TAG, "Map tint setting changed: $mapTint")
                if (mapInitialized) {
                    applyMapTint(mapTint)
                }
            }
        }
    }

    /**
     * Applies a color tint filter to the map tiles for visual customization.
     *
     * @param mapTint The tint configuration to apply
     */
    private fun applyMapTint(mapTint: SettingsManager.MapTint) {
        if (!::map.isInitialized) {
            Log.w(Constants.App.LOG_TAG, "Cannot apply map tint - map not initialized")
            return
        }

        try {
            if (mapTint == SettingsManager.MapTint.NONE) {
                // Remove any existing tint filter
                map.overlayManager.tilesOverlay.setColorFilter(null)
                Log.d(Constants.App.LOG_TAG, "Removed map tint filter")
            } else {
                // Apply the specified tint with 30% intensity
                val tintFilter: ColorMatrixColorFilter = getTintFilter(mapTint.colorValue, 0.3f)
                map.overlayManager.tilesOverlay.setColorFilter(tintFilter)
                Log.d(
                    Constants.App.LOG_TAG,
                    "Applied map tint filter with color: ${mapTint.colorValue}"
                )
            }
            map.invalidate()
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error applying map tint", e)
        }
    }

    /**
     * Updates the location overlay based on current settings and preferences.
     */
    private fun updateLocationOverlay() {
        Log.d(Constants.App.LOG_TAG, "Updating location overlay")
        cleanupLocationOverlay()

        if (shouldShowLocationOverlay()) {
            setupLocationOverlay()
        }

        onLocationOverlayAvailabilityChanged(locationOverlay != null)
        map.invalidate()
    }

    /**
     * Safely cleans up existing location overlay and provider resources.
     */
    private fun cleanupLocationOverlay() {
        locationOverlay?.let { overlay: MyLocationNewOverlay ->
            try {
                Log.d(Constants.App.LOG_TAG, "Cleaning up location overlay")
                overlay.disableMyLocation()
                overlay.disableFollowLocation()
                map.overlays.remove(overlay)
            } catch (e: Exception) {
                Log.w(Constants.App.LOG_TAG, "Error during location overlay cleanup", e)
            }
            locationOverlay = null
        }

        locationProvider?.stopLocationProvider()
        locationProvider = null
    }

    /**
     * Determines whether the location overlay should be displayed based on map state and user settings.
     *
     * @return true if location overlay should be shown, false otherwise
     */
    private fun shouldShowLocationOverlay(): Boolean {
        return ::map.isInitialized && SettingsManager.showMyLocationOnMapFlow.value
    }

    /**
     * Sets up the initial map configuration including tiles, zoom levels, and overlays.
     */
    private fun setupMapConfiguration() {
        Log.d(Constants.App.LOG_TAG, "Setting up map configuration")

        // Configure OpenStreetMap settings
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            cacheMapTileCount = Constants.Map.Cache.TILE_COUNT
            cacheMapTileOvershoot = Constants.Map.Cache.TILE_OVERSHOOT
        }

        try {
            map = MapView(context)

            val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            map.layoutParams = params

            mapContainer.addView(map)

            // Configure map properties
            map.setTileSource(TileSourceFactory.MAPNIK)
            map.setMultiTouchControls(true)

            map.minZoomLevel = Constants.Map.MIN_ZOOM_LEVEL
            map.maxZoomLevel = Constants.Map.MAX_ZOOM_LEVEL

            // Set scrollable area to prevent panning outside defined bounds
            map.setScrollableAreaLimitDouble(Constants.Map.BOUNDS)

            // Hide default zoom controls
            map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

            // Restore saved map position and zoom level
            val savedCenter: GeoPoint = SettingsManager.getSavedMapCenter()
            val savedZoom: Double = SettingsManager.getSavedZoomLevel()

            map.controller.setZoom(savedZoom)
            map.controller.setCenter(savedCenter)

            currentZoomLevel = savedZoom

            // Initialize map components
            setupClusterer()
            setupLocationOverlay()
            setupTouchDetectionOverlay()
            setupMapClickListener()

            map.addMapListener(this)

            // Apply initial tint based on current settings
            applyMapTint(SettingsManager.getCurrentMapTint())

            mapInitialized = true
            Log.d(Constants.App.LOG_TAG, "Map configuration completed successfully")

            // Load initial gas station data
            context.lifecycleScope.launch {
                loadStationsInCurrentView()
            }
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error setting up map configuration", e)
        }
    }

    /**
     * Sets up an overlay to detect user touch interactions for smart follow mode management.
     */
    private fun setupTouchDetectionOverlay() {
        Log.d(Constants.App.LOG_TAG, "Setting up touch detection overlay")

        val touchDetectionOverlay = object : Overlay() {
            override fun onTouchEvent(event: MotionEvent?, mapView: MapView?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // User started touching the map
                        hasUserTouchInteraction = true
                        lastUserTouchTime = System.currentTimeMillis()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // User is dragging on the map
                        if (hasUserTouchInteraction) {
                            lastUserTouchTime = System.currentTimeMillis()
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // User finished touching the map
                        hasUserTouchInteraction = false
                    }
                }
                return false // Allow other overlays to handle the event
            }
        }

        map.overlays.add(touchDetectionOverlay)
    }

    /**
     * Saves the current map position and zoom level to persistent storage with debouncing.
     */
    private fun saveCurrentMapPosition() {
        if (::map.isInitialized) {
            val currentCenter: GeoPoint = map.mapCenter as GeoPoint
            val currentZoom: Double = map.zoomLevelDouble

            // Debounce saving to avoid excessive writes during continuous movement
            debounceHelper.debounce(1000L, context.lifecycleScope) {
                Log.d(
                    Constants.App.LOG_TAG,
                    "Saving map position: lat=${currentCenter.latitude}, lon=${currentCenter.longitude}, zoom=$currentZoom"
                )
                SettingsManager.saveMapPosition(currentCenter, currentZoom)
            }
        }
    }

    /**
     * Sets up an overlay to handle map click events.
     */
    private fun setupMapClickListener() {
        Log.d(Constants.App.LOG_TAG, "Setting up map click listener")

        val mapClickOverlay = object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
                Log.d(Constants.App.LOG_TAG, "Map clicked")
                onMapClick()
                return false // Allow other overlays to handle the event
            }
        }

        map.overlays.add(mapClickOverlay)
    }

    /**
     * Zooms the map in by one level.
     */
    fun zoomIn() {
        if (::map.isInitialized) {
            Log.d(Constants.App.LOG_TAG, "Zooming in")
            map.controller.zoomIn()
        } else {
            Log.w(Constants.App.LOG_TAG, "Cannot zoom in - map not initialized")
        }
    }

    /**
     * Zooms the map out by one level.
     */
    fun zoomOut() {
        if (::map.isInitialized) {
            Log.d(Constants.App.LOG_TAG, "Zooming out")
            map.controller.zoomOut()
        } else {
            Log.w(Constants.App.LOG_TAG, "Cannot zoom out - map not initialized")
        }
    }

    /**
     * Centers the map on the user's current location and toggles follow mode.
     */
    fun centerOnMyLocation() {
        if (!::map.isInitialized) {
            Log.w(Constants.App.LOG_TAG, "Cannot center on location - map not initialized")
            return
        }

        if (!SettingsManager.showMyLocationOnMapFlow.value) {
            Log.w(
                Constants.App.LOG_TAG,
                "Cannot center on location - location display disabled in settings"
            )
            return
        }

        if (locationOverlay == null) {
            Log.d(Constants.App.LOG_TAG, "Setting up location overlay for centering")
            setupLocationOverlay()
        }

        if (isFollowingLocation) {
            Log.d(Constants.App.LOG_TAG, "Disabling follow mode")
            disableFollowMode()
        } else {
            locationOverlay?.let { overlay: MyLocationNewOverlay ->
                val lastKnownLocation: GeoPoint? = overlay.myLocation
                if (lastKnownLocation != null) {
                    Log.d(
                        Constants.App.LOG_TAG,
                        "Centering on known location and enabling follow mode"
                    )
                    enableFollowMode()
                    map.controller.animateTo(lastKnownLocation)
                    context.lifecycleScope.launch {
                        loadStationsInCurrentView()
                    }
                } else {
                    Log.d(Constants.App.LOG_TAG, "Waiting for GPS fix to center on location")
                    // Wait for GPS fix before centering
                    overlay.runOnFirstFix {
                        context.runOnUiThread {
                            val currentLocation: GeoPoint? = overlay.myLocation
                            if (currentLocation != null) {
                                Log.d(
                                    Constants.App.LOG_TAG,
                                    "GPS fix obtained, centering and enabling follow mode"
                                )
                                enableFollowMode()
                                map.controller.animateTo(currentLocation)
                                context.lifecycleScope.launch {
                                    loadStationsInCurrentView()
                                }
                            } else {
                                Log.w(
                                    Constants.App.LOG_TAG,
                                    "GPS fix obtained but location is null"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Enables location follow mode with optimized update intervals.
     */
    private fun enableFollowMode() {
        if (isFollowingLocation) {
            Log.d(Constants.App.LOG_TAG, "Follow mode already enabled")
            return
        }

        Log.d(Constants.App.LOG_TAG, "Enabling follow mode")
        isFollowingLocation = true

        locationOverlay?.enableFollowLocation()

        // Configure location provider for follow mode with optimized update intervals
        locationProvider?.let { provider: GpsMyLocationProvider ->
            provider.locationUpdateMinTime = 1000L
            provider.locationUpdateMinDistance = 1.0f
        }

        onFollowModeChanged(true)
    }

    /**
     * Disables location follow mode.
     */
    private fun disableFollowMode() {
        if (!isFollowingLocation) {
            Log.d(Constants.App.LOG_TAG, "Follow mode already disabled")
            return
        }

        Log.d(Constants.App.LOG_TAG, "Disabling follow mode")
        isFollowingLocation = false
        locationOverlay?.disableFollowLocation()
        onFollowModeChanged(false)
    }

    /**
     * Sets up the gas station clustering system for efficient marker management.
     */
    private fun setupClusterer() {
        try {
            Log.d(Constants.App.LOG_TAG, "Setting up gas station clusterer")
            clusterer = GasStationCluster(context)

            clusterer?.let { cluster: GasStationCluster ->
                updateClustererRadius()
                map.overlays.add(cluster)
                Log.d(Constants.App.LOG_TAG, "Gas station clusterer added to map")
            }

        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error setting up clusterer", e)
        }
    }

    /**
     * Updates the clustering radius based on current zoom level for optimal grouping.
     */
    private fun updateClustererRadius() {
        clusterer?.let { cluster: GasStationCluster ->
            // Adjust clustering radius based on zoom level for better visual grouping
            val radius: Int = when {
                currentZoomLevel < 10 -> Constants.Map.Cluster.CLUSTER_GROUP_RADIUS
                currentZoomLevel < 15 -> (Constants.Map.Cluster.CLUSTER_GROUP_RADIUS * 0.65).toInt()
                else -> (Constants.Map.Cluster.CLUSTER_GROUP_RADIUS * 0.35).toInt()
            }
            cluster.setRadius(radius)
            Log.d(
                Constants.App.LOG_TAG,
                "Updated clusterer radius to $radius for zoom level $currentZoomLevel"
            )
        }
    }

    /**
     * Handles map zoom events and triggers appropriate updates.
     *
     * @param event The zoom event containing zoom level information
     *
     * @return true to indicate the event was handled
     */
    override fun onZoom(event: ZoomEvent?): Boolean {
        event?.let { zoomEvent: ZoomEvent ->
            val newZoomLevel: Double = zoomEvent.zoomLevel

            // Only update if zoom level change is significant to avoid excessive updates
            if (abs(newZoomLevel - currentZoomLevel) > 0.3) {
                Log.d(
                    Constants.App.LOG_TAG,
                    "Significant zoom change: $currentZoomLevel -> $newZoomLevel"
                )
                currentZoomLevel = newZoomLevel
                updateClustererRadius()

                // Debounce station loading to avoid excessive network requests during zoom gestures
                debounceHelper.debounce(200L, context.lifecycleScope) {
                    loadStationsInCurrentView()
                }

                saveCurrentMapPosition()
            }
        }

        return true
    }

    /**
     * Handles map scroll events and manages follow mode based on user interaction.
     *
     * @param event The scroll event (can be null)
     *
     * @return true to indicate the event was handled
     */
    override fun onScroll(event: ScrollEvent?): Boolean {
        val currentTime: Long = System.currentTimeMillis()
        val wasRecentUserTouch: Boolean = (currentTime - lastUserTouchTime) <= 100L

        // Disable follow mode if user manually scrolls the map
        if (isFollowingLocation && hasUserTouchInteraction && wasRecentUserTouch) {
            Log.d(Constants.App.LOG_TAG, "User scrolled map, disabling follow mode")
            disableFollowMode()
        }

        saveCurrentMapPosition()

        // Smart loading: check if we need to load new stations based on current view
        lastLoadedBounds?.let { loadedBounds: BoundingBox ->
            val currentCenter: IGeoPoint = map.mapCenter
            if (!loadedBounds.contains(currentCenter)) {
                // Current view is outside previously loaded bounds, need to load new data
                debounceHelper.debounce(100L, context.lifecycleScope) {
                    loadStationsInCurrentView()
                }
            }
        } ?: run {
            // No previous bounds, load data with longer debounce
            debounceHelper.debounce(300L, context.lifecycleScope) {
                loadStationsInCurrentView()
            }
        }

        return true
    }

    /**
     * Handles location updates when in follow mode.
     *
     * @param location The new location (can be null)
     * @param source The location provider source (can be null)
     */
    override fun onLocationChanged(location: Location?, source: IMyLocationProvider?) {
        if (location == null || !isFollowingLocation) return

        val currentTime: Long = System.currentTimeMillis()

        // Throttle location updates to avoid excessive map movements
        if (currentTime - lastLocationUpdate < 2000L) return

        lastLocationUpdate = currentTime
        Log.d(
            Constants.App.LOG_TAG,
            "Location updated in follow mode: lat=${location.latitude}, lon=${location.longitude}"
        )

        val geoPoint = GeoPoint(location.latitude, location.longitude)
        context.runOnUiThread {
            map.controller.animateTo(geoPoint)

            // Load new stations around the updated location
            context.lifecycleScope.launch {
                debounceHelper.debounce(500L, context.lifecycleScope) {
                    loadStationsInCurrentView()
                }
            }
        }
    }

    /**
     * Loads gas stations for the current map view with intelligent bounds expansion.
     */
    private suspend fun loadStationsInCurrentView() {
        if (!mapInitialized) {
            Log.w(Constants.App.LOG_TAG, "Cannot load stations - map not initialized")
            return
        }

        val currentBounds: BoundingBox = map.boundingBox

        // Adjust buffer size based on zoom level for optimal data loading
        val buffer: Double = when {
            currentZoomLevel < 8 -> Constants.Map.BOUNDS_BUFFER
            currentZoomLevel < 12 -> Constants.Map.BOUNDS_BUFFER / 2
            else -> Constants.Map.BOUNDS_BUFFER / 10
        }

        try {
            // Expand bounds to load stations slightly outside current view for smoother scrolling
            val expandedBounds = BoundingBox(
                currentBounds.latNorth + buffer,
                currentBounds.lonEast + buffer,
                currentBounds.latSouth - buffer,
                currentBounds.lonWest - buffer
            )

            Log.d(
                Constants.App.LOG_TAG,
                "Loading stations for zoom level $currentZoomLevel with buffer $buffer"
            )
            val gasStations: List<GasStation> =
                Constants.App.APP_REPOSITORY.getStationsForZoomLevel(
                    expandedBounds,
                    currentZoomLevel
                )

            loadGasStationMarkers(gasStations)
            lastLoadedBounds = currentBounds
            Log.d(Constants.App.LOG_TAG, "Loaded ${gasStations.size} gas stations")
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error loading stations for current view", e)
        }
    }

    /**
     * Loads gas station markers into the clusterer with validation.
     *
     * @param gasStations List of gas stations to display on the map
     */
    private fun loadGasStationMarkers(gasStations: List<GasStation>) {
        if (!mapInitialized) {
            Log.w(Constants.App.LOG_TAG, "Cannot load markers - map not initialized")
            return
        }

        try {
            clusterer?.let { cluster: GasStationCluster ->
                cluster.items.clear()

                // Filter out stations outside the allowed map bounds
                val validStations: List<GasStation> = gasStations.filter { station: GasStation ->
                    val point = GeoPoint(station.latitude, station.longitude)
                    Constants.Map.BOUNDS.contains(point)
                }

                Log.d(
                    Constants.App.LOG_TAG,
                    "Creating markers for ${validStations.size} valid stations"
                )

                // Create markers for each valid station
                for (station: GasStation in validStations) {
                    val marker = GasStationMarker(map, station, context)

                    marker.setOnMarkerClickListener { _, _ ->
                        Log.d(Constants.App.LOG_TAG, "Gas station marker clicked: ${station.name}")
                        onGasStationClick(station)
                        true
                    }

                    cluster.add(marker)
                }

                cluster.invalidate()
                map.invalidate()
            }
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error loading gas station markers", e)
        }
    }

    /**
     * Sets up the location overlay for displaying user position on the map.
     */
    private fun setupLocationOverlay() {
        if (!shouldShowLocationOverlay()) {
            Log.d(Constants.App.LOG_TAG, "Location overlay not needed")
            onLocationOverlayAvailabilityChanged(false)
            return
        }

        try {
            Log.d(Constants.App.LOG_TAG, "Setting up location overlay")
            cleanupLocationOverlay()

            locationProvider = GpsMyLocationProvider(context)
            locationOverlay = MyLocationNewOverlay(locationProvider, map)

            locationOverlay?.let { overlay: MyLocationNewOverlay ->
                overlay.enableMyLocation()
                map.overlays.add(overlay)
                onLocationOverlayAvailabilityChanged(true)
                Log.d(Constants.App.LOG_TAG, "Location overlay setup completed")
            }
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error setting up location overlay", e)
            onLocationOverlayAvailabilityChanged(false)
        }
    }

    /**
     * Creates a color matrix filter for applying tints to map tiles.
     *
     * @param destinationColor The target color for tinting
     * @param intensity The intensity of the tint effect (0.0 to 1.0)
     *
     * @return A ColorMatrixColorFilter that applies the specified tint
     */
    fun getTintFilter(
        destinationColor: Int,
        intensity: Float = 1.0f
    ): ColorMatrixColorFilter {
        val i: Float = intensity.coerceIn(0.0f, 1.0f)

        val r: Float = Color.red(destinationColor) / 255.0f
        val g: Float = Color.green(destinationColor) / 255.0f
        val b: Float = Color.blue(destinationColor) / 255.0f

        // Luminance coefficients for color conversion
        val lumR = 0.2126f
        val lumG = 0.7152f
        val lumB = 0.0722f

        // Create color matrix for tinting effect
        val colorMatrix = floatArrayOf(
            lumR + (lumR * r - lumR) * i,
            lumG + (lumG * r - lumG) * i,
            lumB + (lumB * r - lumB) * i, 0f, 0f,
            lumR + (lumR * g - lumR) * i,
            lumG + (lumG * g - lumG) * i,
            lumB + (lumB * g - lumB) * i, 0f, 0f,
            lumR + (lumR * b - lumR) * i,
            lumG + (lumG * b - lumG) * i,
            lumB + (lumB * b - lumB) * i, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )

        return ColorMatrixColorFilter(colorMatrix)
    }

    /**
     * Calculates the bounding box that contains all provided gas stations.
     *
     * @param stations List of gas stations to calculate bounds for
     *
     * @return [BoundingBox] that encompasses all stations
     */
    private fun calculateBounds(stations: List<GasStation>): BoundingBox {
        if (stations.isEmpty()) {
            Log.w(Constants.App.LOG_TAG, "Cannot calculate bounds for empty station list")
            return map.boundingBox
        }

        val latitudes: List<Double> = stations.map { it.latitude }
        val longitudes: List<Double> = stations.map { it.longitude }

        return BoundingBox(
            latitudes.maxOrNull() ?: 0.0,
            longitudes.maxOrNull() ?: 0.0,
            latitudes.minOrNull() ?: 0.0,
            longitudes.minOrNull() ?: 0.0
        )
    }

    /**
     * Searches for gas stations with filters and displays results on the map.
     *
     * @param query The search query string
     * @param filters The search filters to apply
     */
    fun searchGasStationsWithFilters(query: String, filters: SearchFilters) {
        if (!mapInitialized) {
            Log.w(Constants.App.LOG_TAG, "Cannot search with filters - map not initialized")
            return
        }

        Log.d(Constants.App.LOG_TAG, "Searching gas stations with query: $query and filters")

        if (isFollowingLocation) {
            disableFollowMode()
        }

        context.lifecycleScope.launch {
            onSearchStateChanged(true)
            try {
                val searchResults: List<GasStation> =
                    Constants.App.APP_REPOSITORY.searchStationsWithFilters(query, filters)

                clusterer?.let { cluster: GasStationCluster ->
                    cluster.items.clear()
                    map.invalidate()
                }

                loadGasStationMarkers(searchResults)

                if (searchResults.isNotEmpty()) {
                    val bounds: BoundingBox = calculateBounds(searchResults)
                    map.zoomToBoundingBox(bounds, true, 100)
                    Log.d(
                        Constants.App.LOG_TAG,
                        "Found ${searchResults.size} stations with filters"
                    )
                } else {
                    Log.d(Constants.App.LOG_TAG, "No stations found with applied filters")
                }
            } catch (e: Exception) {
                Log.e(Constants.App.LOG_TAG, "Error searching gas stations with filters", e)
            } finally {
                onSearchStateChanged(false)
            }
        }
    }

    /**
     * Clears search results and returns to normal map view.
     */
    fun clearSearch() {
        Log.d(Constants.App.LOG_TAG, "Clearing search results")
        context.lifecycleScope.launch {
            loadStationsInCurrentView()
        }
    }

    /**
     * Searches for saved gas stations with filters and displays results on the map.
     *
     * @param query The search query string
     * @param filters The search filters to apply
     */
    fun searchSavedGasStationsWithFilters(query: String, filters: SearchFilters) {
        if (!mapInitialized) {
            Log.w(Constants.App.LOG_TAG, "Cannot search saved stations - map not initialized")
            return
        }

        Log.d(Constants.App.LOG_TAG, "Searching saved gas stations with query: $query and filters")

        if (isFollowingLocation) {
            disableFollowMode()
        }

        context.lifecycleScope.launch {
            onSearchStateChanged(true)
            try {
                val searchResults: List<GasStation> =
                    Constants.App.APP_REPOSITORY.searchSavedStationsWithFilters(query, filters)

                clusterer?.let { cluster: GasStationCluster ->
                    cluster.items.clear()
                    map.invalidate()
                }

                loadGasStationMarkers(searchResults)

                if (searchResults.isNotEmpty()) {
                    val bounds: BoundingBox = calculateBounds(searchResults)
                    map.zoomToBoundingBox(bounds, true, 100)
                    Log.d(
                        Constants.App.LOG_TAG,
                        "Found ${searchResults.size} saved stations with filters"
                    )
                } else {
                    Log.d(Constants.App.LOG_TAG, "No saved stations found with applied filters")
                }
            } catch (e: Exception) {
                Log.e(Constants.App.LOG_TAG, "Error searching saved gas stations with filters", e)
            } finally {
                onSearchStateChanged(false)
            }
        }
    }

    /*****
     * // Handles map resume lifecycle event.
     * fun onResume() {
     *     if (!mapInitialized) {
     *         Log.w(Constants.App.LOG_TAG, "Cannot resume - map not initialized")
     *         return
     *     }

     *     try {
     *         Log.d(Constants.App.LOG_TAG, "Resuming map")
     *         map.onResume()

     *         // Restore location overlay if needed
     *         if (shouldShowLocationOverlay()) {
     *             locationOverlay?.let { overlay: MyLocationNewOverlay ->
     *                 if (overlay.myLocationProvider != null) {
     *                     overlay.enableMyLocation()

     *                     if (isFollowingLocation) {
     *                         overlay.enableFollowLocation()
     *                     }
     *                 } else {
     *                     setupLocationOverlay()
     *                 }
     *             } ?: run {
     *                 setupLocationOverlay()
     *             }
     *         }
     *     } catch (e: Exception) {
     *         Log.e(Constants.App.LOG_TAG, "Error resuming map", e)
     *     }
     * }

     * // Handles map pause lifecycle event.
     * fun onPause() {
     *     if (!mapInitialized) {
     *         Log.w(Constants.App.LOG_TAG, "Cannot pause - map not initialized")
     *         return
     *     }

     *     try {
     *         Log.d(Constants.App.LOG_TAG, "Pausing map")
     *         map.onPause()
     *         saveCurrentMapPosition()

     *         // Disable location services to save battery
     *         locationOverlay?.let { overlay: MyLocationNewOverlay ->
     *             overlay.disableMyLocation()
     *             overlay.disableFollowLocation()
     *         }
     *     } catch (e: Exception) {
     *         Log.e(Constants.App.LOG_TAG, "Error pausing map", e)
     *     }
     * }
     *****/
}