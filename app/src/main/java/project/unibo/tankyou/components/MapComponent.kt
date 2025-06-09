package project.unibo.tankyou.components

import android.Manifest
import android.content.pm.PackageManager
import android.view.MotionEvent
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
import org.osmdroid.views.overlay.Overlay
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
 * @param onMapClick callback chiamato quando si clicca sulla mappa
 */
class MapComponent(
    private val context: AppCompatActivity,
    private val mapContainer: RelativeLayout,
    private val onMapClick: () -> Unit = {}
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

            // Nascondere i controlli zoom di default
            map.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)

            map.controller.setZoom(Constants.Map.DEFAULT_ZOOM_LEVEL)
            map.controller.setCenter(Constants.Map.DEFAULT_GEO_POINT)

            setupClusterer()
            setupMapClickListener()
            map.addMapListener(this)
            setupLocationOverlay()

            mapInitialized = true
        } catch (e: Exception) {
            e
        }
    }

    /**
     * Configura il listener per i click sulla mappa
     */
    private fun setupMapClickListener() {
        val mapClickOverlay = object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
                // Chiamare il callback quando si clicca sulla mappa
                onMapClick()
                return true
            }
        }

        map.overlays.add(0, mapClickOverlay) // Aggiungere all'inizio per avere priorità
    }

    /**
     * Metodi pubblici per controllo zoom dai FAB
     */
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

    fun onResume() {
        if (!mapInitialized) return
        map.onResume()
    }

    fun onPause() {
        if (!mapInitialized) return
        map.onPause()
    }
}