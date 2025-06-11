package project.unibo.tankyou.components

import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.view.MotionEvent
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
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
import project.unibo.tankyou.utils.Debouncer
import project.unibo.tankyou.utils.SettingsManager
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
    private val debounceHelper = Debouncer()

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

            map.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)

            map.controller.setZoom(Constants.Map.DEFAULT_ZOOM_LEVEL)
            map.controller.setCenter(Constants.Map.DEFAULT_GEO_POINT)

            // map.overlayManager.tilesOverlay.setColorFilter(
            //     getTintFilter(
            //         "#FFA0A0A0".toColorInt(),
            //         0.5f
            //     )
            // )

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
     * Listener Configuration
     */
    private fun setupMapClickListener() {
        val mapClickOverlay = object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
                onMapClick()
                return true
            }
        }

        map.overlays.add(0, mapClickOverlay)
    }

    /**
     * Zoom In
     */
    fun zoomIn() {
        if (::map.isInitialized) {
            map.controller.zoomIn()
        }
    }

    /**
     * Zoom Out
     */
    fun zoomOut() {
        if (::map.isInitialized) {
            map.controller.zoomOut()
        }
    }

    /**
     * Center map on user's current location
     */
    fun centerOnMyLocation() {
        if (::map.isInitialized && locationOverlay != null &&
            SettingsManager.shouldUseLocation()
        ) {
            locationOverlay?.let { overlay ->
                val lastKnownLocation = overlay.myLocation
                if (lastKnownLocation != null) {
                    map.controller.animateTo(lastKnownLocation)
                }
            }
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
                    val marker = GasStationMarker(map, station, context)

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
        if (SettingsManager.shouldUseLocation() &&
            SettingsManager.isLocationPermissionGranted(context)
        ) {

            locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
            locationOverlay?.enableMyLocation()
            map.overlays.add(locationOverlay)
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

    /**
     * Map Color Filter Getter
     *
     * @param destinationColor Destination Monochromatic Color
     * @param intensity Tint Intensity
     *
     * @return a [ColorMatrixColorFilter]
     */
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
}