package project.unibo.tankyou.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.RelativeLayout

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope

import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.utils.Constants
import project.unibo.tankyou.utils.DebounceManager

import kotlin.math.abs

/**
 * Component that manages the OpenStreetMap view and gas station markers.
 * Handles map initialization, clustering, location services, and user interactions.
 *
 * @param context the [AppCompatActivity] context
 * @param mapContainer the [RelativeLayout] container for the map
 */
class MapComponent(
    private val context: AppCompatActivity,
    private val mapContainer: RelativeLayout
) : MapListener {
    /** Current TankYou Map */
    private lateinit var map: MapView

    private var locationOverlay: MyLocationNewOverlay? = null
    private var clusterer: GasStationCluster? = null
    private var mapInitialized = false
    private var currentZoomLevel: Double = Constants.Map.DEFAULT_ZOOM_LEVEL
    private var lastLoadedBounds: BoundingBox? = null
    private val debounceHelper = DebounceManager()

    init {
        setupMapConfiguration()
    }

    /**
     * Configures and initializes the map with default settings.
     */
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

            map.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.ALWAYS)
            map.zoomController.setZoomInEnabled(true)
            map.zoomController.setZoomOutEnabled(true)

            map.controller.setZoom(Constants.Map.DEFAULT_ZOOM_LEVEL)
            map.controller.setCenter(Constants.Map.DEFAULT_GEO_POINT)

            setupClusterer()

            map.addMapListener(this)

            setupLocationOverlay()

            mapInitialized = true
        } catch (e: Exception) {
            e
        }
    }

    /**
     * Sets up the marker clustering functionality for gas stations.
     */
    private fun setupClusterer() {
        try {
            clusterer = GasStationCluster(context)

            clusterer?.let { cluster ->
                updateClustererRadius()
                map.overlays.add(cluster)
            }

        } catch (e: Exception) {
            e
        }
    }

    /**
     * Updates the clustering radius based on current zoom level.
     */
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

    /**
     * Handles map zoom events and updates clustering accordingly.
     * @param event the ZoomEvent containing zoom information
     * @return true if the event was handled
     */
    override fun onZoom(event: ZoomEvent?): Boolean {
        event?.let { zoomEvent ->
            val newZoomLevel = zoomEvent.zoomLevel

            if (abs(newZoomLevel - currentZoomLevel) > 0.3) {
                currentZoomLevel = newZoomLevel
                updateClustererRadius()

                debounceHelper.debounce(200L, context.lifecycleScope) {
                    loadStationsInCurrentView()
                }
            }
        }

        return true
    }

    /**
     * Handles map scroll events and loads stations when needed.
     * @param event the ScrollEvent containing scroll information
     * @return true if the event was handled
     */
    override fun onScroll(event: ScrollEvent?): Boolean {
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

    /**
     * Loads gas stations within the current map view bounds.
     */
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
            e
        }
    }

    /**
     * Adds gas station markers to the map with clustering optimization.
     *
     * @param gasStations the list of gas stations to display
     */
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
                    val marker = GasStationMarker(map, station)

                    marker.setOnMarkerClickListener { clickedMarker, _ ->
                        false
                    }

                    cluster.add(marker)
                }

                cluster.invalidate()
                map.invalidate()

            }
        } catch (e: Exception) {
            e
        }
    }

    /**
     * Sets up the user location overlay if location permissions are granted.
     */
    private fun setupLocationOverlay() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val locationProvider = GpsMyLocationProvider(context)

                locationOverlay = MyLocationNewOverlay(locationProvider, map)
                locationOverlay?.enableMyLocation()
                locationOverlay?.enableFollowLocation()
                locationOverlay?.isDrawAccuracyEnabled = true

                map.overlays.add(locationOverlay)

            } catch (e: Exception) {
                e
            }
        }
    }

    /**
     * Resumes map operations when the activity resumes.
     */
    fun onResume() {
        if (!mapInitialized) return
        map.onResume()
    }

    /**
     * Pauses map operations when the activity pauses.
     */
    fun onPause() {
        if (!mapInitialized) return
        map.onPause()
    }
}