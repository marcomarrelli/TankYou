package project.unibo.tankyou.ui.components

import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.location.Location
import android.view.MotionEvent
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
    private var mapInitialized = false
    private var currentZoomLevel: Double = Constants.Map.DEFAULT_ZOOM_LEVEL
    private var lastLoadedBounds: BoundingBox? = null
    private val debounceHelper = Debouncer()

    private var isFollowingLocation = false
    private var lastLocationUpdate: Long = 0

    private var hasUserTouchInteraction = false
    private var lastUserTouchTime = 0L

    init {
        setupMapConfiguration()
        setupSettingsObserver()
    }

    fun isFollowModeActive(): Boolean = isFollowingLocation

    private fun setupSettingsObserver() {
        context.lifecycleScope.launch {
            SettingsManager.showMyLocationOnMapFlow.collect { showLocation ->
                if (mapInitialized) {
                    updateLocationOverlay()
                }
            }
        }

        context.lifecycleScope.launch {
            SettingsManager.mapTintFlow.collect { mapTint ->
                if (mapInitialized) {
                    applyMapTint(mapTint)
                }
            }
        }
    }

    private fun applyMapTint(mapTint: SettingsManager.MapTint) {
        if (!::map.isInitialized) return

        try {
            if (mapTint == SettingsManager.MapTint.NONE) {
                map.overlayManager.tilesOverlay.setColorFilter(null)
            } else {
                val tintFilter = getTintFilter(mapTint.colorValue, 0.3f)
                map.overlayManager.tilesOverlay.setColorFilter(tintFilter)
            }
            map.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateLocationOverlay() {
        cleanupLocationOverlay()

        if (shouldShowLocationOverlay()) {
            setupLocationOverlay()
        }

        onLocationOverlayAvailabilityChanged(locationOverlay != null)
        map.invalidate()
    }

    private fun cleanupLocationOverlay() {
        locationOverlay?.let { overlay ->
            try {
                overlay.disableMyLocation()
                overlay.disableFollowLocation()
                map.overlays.remove(overlay)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
            locationOverlay = null
        }

        locationProvider?.stopLocationProvider()
        locationProvider = null
    }

    private fun shouldShowLocationOverlay(): Boolean {
        return ::map.isInitialized && SettingsManager.showMyLocationOnMapFlow.value
    }

    private fun setupMapConfiguration() {
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

            map.setTileSource(TileSourceFactory.MAPNIK)
            map.setMultiTouchControls(true)

            map.minZoomLevel = Constants.Map.MIN_ZOOM_LEVEL
            map.maxZoomLevel = Constants.Map.MAX_ZOOM_LEVEL

            map.setScrollableAreaLimitDouble(Constants.Map.BOUNDS)

            map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

            val savedCenter = SettingsManager.getSavedMapCenter()
            val savedZoom = SettingsManager.getSavedZoomLevel()

            map.controller.setZoom(savedZoom)
            map.controller.setCenter(savedCenter)

            currentZoomLevel = savedZoom

            setupClusterer()
            setupLocationOverlay()
            setupTouchDetectionOverlay()
            setupMapClickListener()

            map.addMapListener(this)

            // Apply initial tint
            applyMapTint(SettingsManager.getCurrentMapTint())

            mapInitialized = true

            context.lifecycleScope.launch {
                loadStationsInCurrentView()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupTouchDetectionOverlay() {
        val touchDetectionOverlay = object : Overlay() {
            override fun onTouchEvent(event: MotionEvent?, mapView: MapView?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        hasUserTouchInteraction = true
                        lastUserTouchTime = System.currentTimeMillis()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (hasUserTouchInteraction) {
                            lastUserTouchTime = System.currentTimeMillis()
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        hasUserTouchInteraction = false
                    }
                }
                return false
            }
        }

        map.overlays.add(touchDetectionOverlay)
    }

    private fun saveCurrentMapPosition() {
        if (::map.isInitialized) {
            val currentCenter: GeoPoint = map.mapCenter as GeoPoint
            val currentZoom: Double = map.zoomLevelDouble

            debounceHelper.debounce(1000L, context.lifecycleScope) {
                SettingsManager.saveMapPosition(currentCenter, currentZoom)
            }
        }
    }

    private fun setupMapClickListener() {
        val mapClickOverlay = object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
                onMapClick()
                return false
            }
        }

        map.overlays.add(mapClickOverlay)
    }

    fun zoomIn() {
        if (::map.isInitialized) {
            map.controller.zoomIn()
        }
    }

    fun zoomOut() {
        if (::map.isInitialized) {
            map.controller.zoomOut()
        }
    }

    fun centerOnMyLocation() {
        if (!::map.isInitialized) return

        if (!SettingsManager.showMyLocationOnMapFlow.value) {
            return
        }

        if (locationOverlay == null) {
            setupLocationOverlay()
        }

        if (isFollowingLocation) {
            disableFollowMode()
        } else {
            locationOverlay?.let { overlay ->
                val lastKnownLocation = overlay.myLocation
                if (lastKnownLocation != null) {
                    enableFollowMode()
                    map.controller.animateTo(lastKnownLocation)
                    context.lifecycleScope.launch {
                        loadStationsInCurrentView()
                    }
                } else {
                    overlay.runOnFirstFix {
                        context.runOnUiThread {
                            val currentLocation = overlay.myLocation
                            if (currentLocation != null) {
                                enableFollowMode()
                                map.controller.animateTo(currentLocation)
                                context.lifecycleScope.launch {
                                    loadStationsInCurrentView()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun enableFollowMode() {
        if (isFollowingLocation) return

        isFollowingLocation = true

        locationOverlay?.enableFollowLocation()

        locationProvider?.let { provider ->
            provider.locationUpdateMinTime = 1000L
            provider.locationUpdateMinDistance = 1.0f
        }

        onFollowModeChanged(true)
    }

    private fun disableFollowMode() {
        if (!isFollowingLocation) return

        isFollowingLocation = false
        locationOverlay?.disableFollowLocation()
        onFollowModeChanged(false)
    }

    private fun setupClusterer() {
        try {
            clusterer = GasStationCluster(context)

            clusterer?.let { cluster ->
                updateClustererRadius()
                map.overlays.add(cluster)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateClustererRadius() {
        clusterer?.let { cluster ->
            val radius: Int = when {
                currentZoomLevel < 10 -> Constants.Map.Cluster.CLUSTER_GROUP_RADIUS
                currentZoomLevel < 15 -> (Constants.Map.Cluster.CLUSTER_GROUP_RADIUS * 0.65).toInt()
                else -> (Constants.Map.Cluster.CLUSTER_GROUP_RADIUS * 0.35).toInt()
            }
            cluster.setRadius(radius)
        }
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        event?.let { zoomEvent ->
            val newZoomLevel = zoomEvent.zoomLevel

            if (abs(newZoomLevel - currentZoomLevel) > 0.3) {
                currentZoomLevel = newZoomLevel
                updateClustererRadius()

                debounceHelper.debounce(200L, context.lifecycleScope) {
                    loadStationsInCurrentView()
                }

                saveCurrentMapPosition()
            }
        }

        return true
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        val currentTime = System.currentTimeMillis()
        val wasRecentUserTouch = (currentTime - lastUserTouchTime) <= 100L

        if (isFollowingLocation && hasUserTouchInteraction && wasRecentUserTouch) {
            disableFollowMode()
        }

        saveCurrentMapPosition()

        lastLoadedBounds?.let { loadedBounds ->
            val currentCenter = map.mapCenter
            if (!loadedBounds.contains(currentCenter)) {
                debounceHelper.debounce(100L, context.lifecycleScope) {
                    loadStationsInCurrentView()
                }
            }
        } ?: run {
            debounceHelper.debounce(300L, context.lifecycleScope) {
                loadStationsInCurrentView()
            }
        }

        return true
    }

    override fun onLocationChanged(location: Location?, source: IMyLocationProvider?) {
        if (location == null || !isFollowingLocation) return

        val currentTime = System.currentTimeMillis()

        if (currentTime - lastLocationUpdate < 2000L) return

        lastLocationUpdate = currentTime

        val geoPoint = GeoPoint(location.latitude, location.longitude)
        context.runOnUiThread {
            map.controller.animateTo(geoPoint)

            context.lifecycleScope.launch {
                debounceHelper.debounce(500L, context.lifecycleScope) {
                    loadStationsInCurrentView()
                }
            }
        }
    }

    private suspend fun loadStationsInCurrentView() {
        if (!mapInitialized) return

        val currentBounds = map.boundingBox

        val buffer = when {
            currentZoomLevel < 8 -> Constants.Map.BOUNDS_BUFFER
            currentZoomLevel < 12 -> Constants.Map.BOUNDS_BUFFER / 2
            else -> Constants.Map.BOUNDS_BUFFER / 10
        }

        try {
            val expandedBounds = BoundingBox(
                currentBounds.latNorth + buffer,
                currentBounds.lonEast + buffer,
                currentBounds.latSouth - buffer,
                currentBounds.lonWest - buffer
            )

            val gasStations = Constants.App.REPOSITORY.getStationsForZoomLevel(
                expandedBounds,
                currentZoomLevel
            )

            loadGasStationMarkers(gasStations)
            lastLoadedBounds = currentBounds
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadGasStationMarkers(gasStations: List<GasStation>) {
        if (!mapInitialized) return

        try {
            clusterer?.let { cluster ->
                cluster.items.clear()

                val validStations = gasStations.filter { station ->
                    val point = GeoPoint(station.latitude, station.longitude)
                    Constants.Map.BOUNDS.contains(point)
                }

                for (station in validStations) {
                    val marker = GasStationMarker(map, station, context)

                    marker.setOnMarkerClickListener { _, _ ->
                        onGasStationClick(station)
                        true
                    }

                    cluster.add(marker)
                }

                cluster.invalidate()
                map.invalidate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupLocationOverlay() {
        if (!shouldShowLocationOverlay()) {
            onLocationOverlayAvailabilityChanged(false)
            return
        }

        try {
            cleanupLocationOverlay()

            locationProvider = GpsMyLocationProvider(context)
            locationOverlay = MyLocationNewOverlay(locationProvider, map)

            locationOverlay?.let { overlay ->
                overlay.enableMyLocation()
                map.overlays.add(overlay)
                onLocationOverlayAvailabilityChanged(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onLocationOverlayAvailabilityChanged(false)
        }
    }

    fun onResume() {
        if (!mapInitialized) return

        try {
            map.onResume()

            if (shouldShowLocationOverlay()) {
                locationOverlay?.let { overlay ->
                    if (overlay.myLocationProvider != null) {
                        overlay.enableMyLocation()

                        if (isFollowingLocation) {
                            overlay.enableFollowLocation()
                        }
                    } else {
                        setupLocationOverlay()
                    }
                } ?: run {
                    setupLocationOverlay()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onPause() {
        if (!mapInitialized) return

        try {
            map.onPause()
            saveCurrentMapPosition()

            locationOverlay?.let { overlay ->
                overlay.disableMyLocation()
                overlay.disableFollowLocation()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTintFilter(
        destinationColor: Int,
        intensity: Float = 1.0f
    ): ColorMatrixColorFilter {
        val i = intensity.coerceIn(0.0f, 1.0f)

        val r = Color.red(destinationColor) / 255.0f
        val g = Color.green(destinationColor) / 255.0f
        val b = Color.blue(destinationColor) / 255.0f

        val lumR = 0.2126f
        val lumG = 0.7152f
        val lumB = 0.0722f

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

    fun searchGasStations(query: String) {
        if (!mapInitialized) return

        if (isFollowingLocation) {
            disableFollowMode()
        }

        context.lifecycleScope.launch {
            onSearchStateChanged(true)
            try {
                val searchResults = Constants.App.REPOSITORY.searchStations(query)

                clusterer?.let { cluster ->
                    cluster.items.clear()
                    map.invalidate()
                }

                loadGasStationMarkers(searchResults)

                if (searchResults.isNotEmpty()) {
                    val bounds = calculateBounds(searchResults)
                    map.zoomToBoundingBox(bounds, true, 100)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onSearchStateChanged(false)
            }
        }
    }

    private fun calculateBounds(stations: List<GasStation>): BoundingBox {
        if (stations.isEmpty()) return map.boundingBox

        val latitudes = stations.map { it.latitude }
        val longitudes = stations.map { it.longitude }

        return BoundingBox(
            latitudes.maxOrNull() ?: 0.0,
            longitudes.maxOrNull() ?: 0.0,
            latitudes.minOrNull() ?: 0.0,
            longitudes.minOrNull() ?: 0.0
        )
    }

    fun searchGasStationsWithFilters(query: String, filters: SearchFilters) {
        if (!mapInitialized) return

        if (isFollowingLocation) {
            disableFollowMode()
        }

        context.lifecycleScope.launch {
            onSearchStateChanged(true)
            try {
                val searchResults =
                    Constants.App.REPOSITORY.searchStationsWithFilters(query, filters)

                clusterer?.let { cluster ->
                    cluster.items.clear()
                    map.invalidate()
                }

                loadGasStationMarkers(searchResults)

                if (searchResults.isNotEmpty()) {
                    val bounds = calculateBounds(searchResults)
                    map.zoomToBoundingBox(bounds, true, 100)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onSearchStateChanged(false)
            }
        }
    }

    fun clearSearch() {
        context.lifecycleScope.launch {
            loadStationsInCurrentView()
        }
    }

    fun searchSavedGasStationsWithFilters(query: String, filters: SearchFilters) {
        if (!mapInitialized) return

        if (isFollowingLocation) {
            disableFollowMode()
        }

        context.lifecycleScope.launch {
            onSearchStateChanged(true)
            try {
                val searchResults =
                    Constants.App.REPOSITORY.searchSavedStationsWithFilters(query, filters)

                clusterer?.let { cluster ->
                    cluster.items.clear()
                    map.invalidate()
                }

                loadGasStationMarkers(searchResults)

                if (searchResults.isNotEmpty()) {
                    val bounds = calculateBounds(searchResults)
                    map.zoomToBoundingBox(bounds, true, 100)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onSearchStateChanged(false)
            }
        }
    }
}