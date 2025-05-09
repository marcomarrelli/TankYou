package project.unibo.tankyou

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
// import org.osmdroid.views.overlay.compass.CompassOverlay
// import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
// import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Prima configura osmdroid
            val ctx: Context = applicationContext
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            Configuration.getInstance().userAgentValue = packageName

            // Poi imposta la view
            setContentView(R.layout.activity_main)

            // Verifica i permessi prima di inizializzare la mappa
            val permissionsGranted = requestPermissionsIfNecessary(arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
            ))

            // Solo se i permessi sono concessi, inizializza la mappa
            if (permissionsGranted) {
                initializeMap()
            }
        } catch (e: Exception) {
            // Log dell'errore per capire cosa sta causando il crash
            Log.e("OSMDroid", "Error initializing app", e)
            Toast.makeText(this, "Errore: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeMap() {
        map = findViewById(R.id.mapView)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // Configurazione iniziale della mappa
        val mapController = map.controller
        mapController.setZoom(15.0)
        val startPoint = GeoPoint(41.9028, 12.4964) // Roma
        mapController.setCenter(startPoint)
    }

    private fun requestPermissionsIfNecessary(permissions: Array<String>): Boolean {
        val permissionsToRequest = ArrayList<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }

            if (allGranted) {
                initializeMap()
            } else {
                Toast.makeText(this, "L'applicazione richiede permessi per funzionare correttamente", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::map.isInitialized) {
            map.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::map.isInitialized) {
            map.onPause()
        }
    }
}