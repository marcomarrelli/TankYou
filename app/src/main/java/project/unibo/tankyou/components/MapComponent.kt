package project.unibo.tankyou.components

import android.Manifest
import android.content.Context
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
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    fun initialize() {
        if (mapInitialized) {
            return
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
            Log.d(TAG, "Mappa inizializzata con successo con OSMBonusPack clustering")

        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'inizializzazione della mappa", e)
        }
    }

    private fun setupClusterer() {
        try {
            clusterer = GasStationCluster(context)

            clusterer?.let { cluster ->
                cluster.setRadius(100)
                map.overlays.add(cluster)
                Log.d(TAG, "Clusterer colorato configurato correttamente")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Errore nella configurazione del clusterer", e)
        }
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        event?.let { zoomEvent ->
            val newZoomLevel = zoomEvent.zoomLevel

            if (abs(newZoomLevel - currentZoomLevel) > 0.5) {
                currentZoomLevel = newZoomLevel

                debounceHelper.debounce(300L, context.lifecycleScope) {
                    loadStationsInCurrentView()
                }
            }
        }
        return true
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        debounceHelper.debounce(500L, context.lifecycleScope) {
            loadStationsInCurrentView()
        }
        return true
    }

    private suspend fun loadStationsInCurrentView() {
        if (!mapInitialized) return

        val currentBounds = map.boundingBox

        try {
            val buffer = 0.1
            val gasStations = appRepository.getStationsForZoomLevel(
                BoundingBox(
                    currentBounds.latNorth + buffer,
                    currentBounds.lonEast + buffer,
                    currentBounds.latSouth - buffer,
                    currentBounds.lonWest - buffer
                ),
                currentZoomLevel
            )

            addMarkersOptimized(gasStations)
            lastLoadedBounds = currentBounds

        } catch (e: Exception) {
            Log.e(TAG, "Errore nel caricamento delle stazioni", e)
        }
    }

    fun loadInitialStations() {
        context.lifecycleScope.launch {
            loadStationsInCurrentView()
        }
    }

    private fun addMarkersOptimized(gasStations: List<GasStation>) {
        if (!mapInitialized) {
            Log.w(TAG, "Mappa non ancora inizializzata, impossibile aggiungere marker")
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
                        Log.d(TAG, "Cliccata stazione: ${station.name}")
                        false
                    }

                    cluster.add(marker)
                }

                cluster.invalidate()
                map.invalidate()

                Log.d(TAG, "Aggiunti ${validStations.size} marker con OSMBonusPack clustering ottimizzato")
            } ?: Log.e(TAG, "Clusterer non inizializzato")

        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'aggiunta dei marker con clustering", e)
        }
    }

    private fun setupLocationOverlay() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
                locationOverlay?.enableMyLocation()
                map.overlays.add(locationOverlay)

                locationOverlay?.runOnFirstFix {
                    context.runOnUiThread {
                        val myLocation = locationOverlay?.myLocation
                        if (myLocation != null && !italyBounds.contains(myLocation)) {
                            map.controller.animateTo(GeoPoint(42.5, 12.5))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore nella configurazione dell'overlay per la posizione", e)
            }
        }
    }

    fun enableLocationFollowing(enable: Boolean) {
        locationOverlay?.let {
            if (enable) {
                it.enableFollowLocation()
            } else {
                it.disableFollowLocation()
            }
        }
    }

    fun setZoom(zoomLevel: Double) {
        val constrainedZoom = zoomLevel.coerceIn(map.minZoomLevel, map.maxZoomLevel)
        map.controller.setZoom(constrainedZoom)
    }

    fun centerOn(latitude: Double, longitude: Double) {
        val point = GeoPoint(latitude, longitude)
        if (italyBounds.contains(point)) {
            map.controller.animateTo(point)
        } else {
            val constrainedLat = latitude.coerceIn(italyBounds.latSouth, italyBounds.latNorth)
            val constrainedLon = longitude.coerceIn(italyBounds.lonWest, italyBounds.lonEast)
            map.controller.animateTo(GeoPoint(constrainedLat, constrainedLon))
            Log.d(TAG, "Coordinate fuori dai confini italiani, riportate ai limiti")
        }
    }

    fun addGasStationMarkers(gasStations: List<GasStation>) {
        addMarkersOptimized(gasStations)
    }

    fun clearGasStationMarkers() {
        if (!mapInitialized) {
            Log.w(TAG, "Mappa non ancora inizializzata")
            return
        }

        try {
            clusterer?.let { cluster ->
                cluster.items.clear()
                cluster.invalidate()
                map.invalidate()
            }
            Log.d(TAG, "Rimossi tutti i marker delle stazioni")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella rimozione dei marker", e)
        }
    }

    fun fitMapToStations(gasStations: List<GasStation>) {
        if (!mapInitialized) {
            Log.w(TAG, "Mappa non ancora inizializzata")
            return
        }

        if (gasStations.isEmpty()) {
            Log.d(TAG, "Nessuna stazione da visualizzare")
            return
        }

        try {
            val validStations = gasStations.filter { station ->
                val point = GeoPoint(station.latitude, station.longitude)
                italyBounds.contains(point)
            }

            if (validStations.isNotEmpty()) {
                val latitudes = validStations.map { it.latitude }
                val longitudes = validStations.map { it.longitude }

                val minLat = latitudes.minOrNull() ?: return
                val maxLat = latitudes.maxOrNull() ?: return
                val minLon = longitudes.minOrNull() ?: return
                val maxLon = longitudes.maxOrNull() ?: return

                val boundingBox = BoundingBox(maxLat, maxLon, minLat, minLon)
                map.zoomToBoundingBox(boundingBox, true, 100)

                Log.d(TAG, "Mappa centrata su ${validStations.size} stazioni")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel centrare la mappa sulle stazioni", e)
        }
    }

    fun onResume() {
        if (::map.isInitialized && mapInitialized) {
            map.onResume()
        }
    }

    fun onPause() {
        if (::map.isInitialized && mapInitialized) {
            map.onPause()
        }
    }
}