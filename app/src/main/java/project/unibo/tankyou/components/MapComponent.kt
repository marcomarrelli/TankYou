package project.unibo.tankyou.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

class MapComponent(
    private val context: AppCompatActivity,
    private val mapContainer: RelativeLayout
) : MapListener {
    private lateinit var map: MapView
    private var locationOverlay: MyLocationNewOverlay? = null
    private lateinit var clusterManager: ClusterManager
    private val TAG: String = "MapComponent"

    private var mapInitialized = false

    // Definizione dei confini geografici dell'Italia
    private val italyBounds = BoundingBox(
        47.5,  // Nord
        19.0,  // Est
        36.0,  // Sud
        6.0    // Ovest
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
            // Crea e configura la MapView
            map = MapView(context)

            // Imposta i parametri di layout
            val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            map.layoutParams = params

            // Aggiungi la mappa al container
            mapContainer.addView(map)

            // Configura la mappa
            map.setTileSource(TileSourceFactory.MAPNIK)
            map.setMultiTouchControls(true)

            // Configurazione zoom
            map.minZoomLevel = 6.0
            map.maxZoomLevel = 19.0

            // Limita la mappa ai confini dell'Italia
            map.setScrollableAreaLimitDouble(italyBounds)

            // Abilita i controlli zoom
            map.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.ALWAYS)
            map.zoomController.setZoomInEnabled(true)
            map.zoomController.setZoomOutEnabled(true)

            // Posizionamento iniziale (Centro Italia)
            val mapController = map.controller
            mapController.setZoom(7.0)
            val italyCenterPoint = GeoPoint(42.5, 12.5)
            mapController.setCenter(italyCenterPoint)

            // Inizializza il cluster manager
            clusterManager = ClusterManager(context, map)

            // Aggiungi listener per aggiornare i cluster quando la mappa cambia
            map.addMapListener(this)

            setupLocationOverlay()

            mapInitialized = true
            Log.d(TAG, "Mappa inizializzata con successo con clustering")

        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'inizializzazione della mappa", e)
        }
    }

    // Implementazione MapListener per aggiornare cluster su zoom/scroll
    override fun onZoom(event: ZoomEvent?): Boolean {
        if (::clusterManager.isInitialized) {
            clusterManager.updateClusters()
        }
        return true
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        // Opzionale: aggiorna cluster anche su scroll se necessario
        return true
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

    // Nuova funzione con clustering
    fun addGasStationMarkersWithClustering(gasStations: List<GasStation>) {
        if (!mapInitialized) {
            Log.w(TAG, "Mappa non ancora inizializzata, impossibile aggiungere marker")
            return
        }

        try {
            // Filtra le stazioni entro i confini italiani
            val validStations = gasStations.filter { station ->
                val point = GeoPoint(station.latitude, station.longitude)
                italyBounds.contains(point)
            }

            if (validStations.isNotEmpty()) {
                clusterManager.setStations(validStations)
                Log.d(TAG, "Aggiunti ${validStations.size} stazioni con clustering")
            } else {
                Log.d(TAG, "Nessuna stazione valida entro i confini italiani")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'aggiunta dei marker con clustering", e)
        }
    }

    // Mantieni anche la funzione originale per compatibilità
    fun addGasStationMarkers(gasStations: List<GasStation>) {
        addGasStationMarkersWithClustering(gasStations)
    }

    fun clearGasStationMarkers() {
        if (!mapInitialized) {
            Log.w(TAG, "Mappa non ancora inizializzata")
            return
        }

        try {
            if (::clusterManager.isInitialized) {
                clusterManager.setStations(emptyList())
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