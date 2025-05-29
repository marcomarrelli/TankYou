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
import org.osmdroid.views.overlay.Marker
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
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
    private var clusterer: RadiusMarkerClusterer? = null
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

            // Inizializza il clusterer
            setupClusterer()

            // Aggiungi listener per aggiornare i cluster quando la mappa cambia
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
            // Crea il clusterer con raggio personalizzato
            clusterer = RadiusMarkerClusterer(context)

            // Configurazione del clusterer
            clusterer?.let { cluster ->
                // Raggio di clustering in pixel (più alto = più raggruppamento)
                cluster.setRadius(100)

                // Icona per i cluster (opzionale)
                // cluster.setIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_cluster))

                // Testo per i cluster
                cluster.textPaint.textSize = 12f * context.resources.displayMetrics.density
                cluster.textPaint.color = android.graphics.Color.WHITE
                cluster.textPaint.isAntiAlias = true

                // Aggiungi il clusterer alla mappa
                map.overlays.add(cluster)

                Log.d(TAG, "Clusterer configurato correttamente")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Errore nella configurazione del clusterer", e)
        }
    }

    // Implementazione MapListener per aggiornare cluster su zoom/scroll
    override fun onZoom(event: ZoomEvent?): Boolean {
        clusterer?.invalidate()
        return true
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        // Il clusterer si aggiorna automaticamente
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

    // Nuova funzione con OSMBonusPack clustering
    fun addGasStationMarkersWithClustering(gasStations: List<GasStation>) {
        if (!mapInitialized) {
            Log.w(TAG, "Mappa non ancora inizializzata, impossibile aggiungere marker")
            return
        }

        try {
            clusterer?.let { cluster ->
                // Pulisce i marker esistenti
                cluster.items.clear()

                // Filtra le stazioni entro i confini italiani
                val validStations = gasStations.filter { station ->
                    val point = GeoPoint(station.latitude, station.longitude)
                    italyBounds.contains(point)
                }

                // Crea i marker per le stazioni valide
                for (station in validStations) {
                    val marker = GasStationMarker(map, station)

                    // Opzionale: aggiungi listener personalizzato
                    marker.setOnMarkerClickListener { clickedMarker, _ ->
                        Log.d(TAG, "Cliccata stazione: ${station.name}")
                        // Qui puoi aggiungere logica personalizzata per il click
                        false // Ritorna false per permettere il comportamento di default (popup)
                    }

                    cluster.add(marker)
                }

                // Aggiorna la visualizzazione
                cluster.invalidate()
                map.invalidate()

                Log.d(TAG, "Aggiunti ${validStations.size} marker con OSMBonusPack clustering")
            } ?: Log.e(TAG, "Clusterer non inizializzato")

        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'aggiunta dei marker con clustering", e)
        }
    }

    // Mantieni la funzione originale per compatibilità
    fun addGasStationMarkers(gasStations: List<GasStation>) {
        addGasStationMarkersWithClustering(gasStations)
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