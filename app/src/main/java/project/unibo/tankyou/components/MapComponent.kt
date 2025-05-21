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

class MapComponent(
    private val context: AppCompatActivity,
    private val mapContainer: RelativeLayout
) {
    private lateinit var map: MapView
    private var locationOverlay: MyLocationNewOverlay? = null
    private val TAG: String = "MapComponent"

    private var mapInitialized = false

    // Definizione dei confini geografici dell'Italia (con un po' di margine)
    private val italyBounds = BoundingBox(
        47.5,  // Nord (include parzialmente Svizzera e Austria per coprire tutto il Nord Italia)
        19.0,  // Est (include costa adriatica e parte dell'Ionio)
        36.0,  // Sud (include Sicilia e Lampedusa)
        6.0    // Ovest (include la Sardegna e il confine con la Francia)
    )

    init {
        // Configurazione iniziale di osmdroid
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
            map.minZoomLevel = 6.0  // Zoom minimo per vedere l'Italia intera
            map.maxZoomLevel = 19.0 // Zoom massimo per vedere i dettagli

            // Limita la mappa ai confini dell'Italia
            map.setScrollableAreaLimitDouble(italyBounds)

            // Limita anche il livello di zoom in base alle dimensioni dell'Italia
            map.setMinZoomLevel(6.0) // Livello di zoom che mostra tutta l'Italia

            // Abilita i controlli zoom
            map.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.ALWAYS)
            map.zoomController.setZoomInEnabled(true)
            map.zoomController.setZoomOutEnabled(true)

            // Posizionamento iniziale (Centro Italia)
            val mapController = map.controller
            mapController.setZoom(7.0)  // Un livello che mostra gran parte dell'Italia
            val italyCenterPoint = GeoPoint(42.5, 12.5) // Centro approssimativo dell'Italia
            mapController.setCenter(italyCenterPoint)

            setupLocationOverlay()

            mapInitialized = true
            Log.d(TAG, "Mappa inizializzata con successo e limitata all'Italia")

        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'inizializzazione della mappa", e)
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

    // fun isInItaly(latitude: Double, longitude: Double): Boolean {
    //     return italyBounds.contains(GeoPoint(latitude, longitude))
    // }
}