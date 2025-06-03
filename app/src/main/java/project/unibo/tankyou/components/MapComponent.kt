package project.unibo.tankyou.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.repository.AppRepository
import project.unibo.tankyou.utils.Constants
import project.unibo.tankyou.utils.DebounceManager
import kotlin.math.abs

class MapComponent(
    private val context: AppCompatActivity,
    private val mapContainer: RelativeLayout
) : MapListener {
    private lateinit var map: MapView
    private var locationOverlay: MyLocationNewOverlay? = null
    private var clusterer: GasStationCluster? = null
    private val TAG: String = "MapComponent"
    private var mapInitialized = false
    private val appRepository = AppRepository.getInstance()
    private var currentZoomLevel = 7.0
    private var lastLoadedBounds: BoundingBox? = null
    private val debounceHelper = DebounceManager()

    private val italyBounds = BoundingBox(
        47.5,
        19.0,
        36.0,
        6.0
    )

    init {
        setupMapConfiguration()
    }

    private fun setupMapConfiguration() {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            cacheMapTileCount = 12
            cacheMapTileOvershoot = 2
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

            map.minZoomLevel = 6.0
            map.maxZoomLevel = 19.0

            map.setScrollableAreaLimitDouble(italyBounds)

            map.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.ALWAYS)
            map.zoomController.setZoomInEnabled(true)
            map.zoomController.setZoomOutEnabled(true)

            val mapController = map.controller
            mapController.setZoom(7.0)
            val italyCenterPoint = GeoPoint(42.5, 12.5)
            mapController.setCenter(italyCenterPoint)

            setupClusterer()
            map.addMapListener(this)
            setupLocationOverlay()

            mapInitialized = true
        } catch (e: Exception) {

        }
    }

    private fun setupClusterer() {
        try {
            clusterer = GasStationCluster(context)

            clusterer?.let { cluster ->
                updateClustererRadius()
                map.overlays.add(cluster)
            }

        } catch (e: Exception) {

        }
    }

    private fun updateClustererRadius() {
        clusterer?.let { cluster ->
            val radius: Int = when {
                currentZoomLevel < 10 -> (Constants.Cluster.CLUSTER_GROUP_RADIUS*0.35).toInt()
                currentZoomLevel < 15 -> (Constants.Cluster.CLUSTER_GROUP_RADIUS*0.65).toInt()
                else -> Constants.Cluster.CLUSTER_GROUP_RADIUS
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
            currentZoomLevel < 8 -> 0.5
            currentZoomLevel < 12 -> 0.2
            else -> 0.05
        }

        try {
            val expandedBounds = BoundingBox(
                currentBounds.latNorth + buffer,
                currentBounds.lonEast + buffer,
                currentBounds.latSouth - buffer,
                currentBounds.lonWest - buffer
            )

            val gasStations = appRepository.getStationsForZoomLevel(
                expandedBounds,
                currentZoomLevel
            )

            addMarkersOptimized(gasStations)
            lastLoadedBounds = currentBounds


        } catch (e: Exception) {
        }
    }

    fun loadInitialStations() {
        context.lifecycleScope.launch {
            loadStationsInCurrentView()
        }
    }

    private fun addMarkersOptimized(gasStations: List<GasStation>) {
        if (!mapInitialized) {
            return
        }

        try {
            clusterer?.let { cluster ->
                cluster.items.clear()

                val validStations = gasStations.filter { station ->
                    val point = GeoPoint(station.latitude, station.longitude)
                    italyBounds.contains(point)
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
            }
        } else {
        }
    }

    fun initialize() {
        if (!mapInitialized) {
            return
        }

        try {
            map.onResume()
        } catch (e: Exception) {
        }
    }

    fun addGasStationMarkers(gasStations: List<GasStation>) {
        addMarkersOptimized(gasStations)
    }

    fun clearGasStationMarkers() {
        clusterer?.items?.clear()
        clusterer?.invalidate()
        map.invalidate()
    }

    fun onResume() {
        if (mapInitialized) {
            map.onResume()
        }
    }

    fun onPause() {
        if (mapInitialized) {
            map.onPause()
        }
    }
}