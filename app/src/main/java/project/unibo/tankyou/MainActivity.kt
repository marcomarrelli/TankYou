package project.unibo.tankyou

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import project.unibo.tankyou.components.MapComponent
import project.unibo.tankyou.data.repository.AppRepository

class MainActivity : AppCompatActivity() {

    private lateinit var mapComponent: MapComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapContainer: RelativeLayout = findViewById(R.id.mapContainer)
        mapComponent = MapComponent(this, mapContainer)

        requestPermissions()

        // Carica e visualizza le stazioni di servizio
        loadAndDisplayGasStations()
    }

    private fun loadAndDisplayGasStations() {
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Inizio caricamento stazioni di servizio...")

                val appRepository = AppRepository()

                // Prima controlla quante stazioni ci sono in totale
                val totalCount = appRepository.getStationCount()
                Log.d("MainActivity", "Stazioni totali nel database: $totalCount")

                // Poi carica quelle che riesci a ottenere
                val gasStations = appRepository.getAllStations()
                Log.d("MainActivity", "Stazioni effettivamente caricate: ${gasStations.size}")

                if (totalCount > gasStations.size) {
                    Log.w("MainActivity", "⚠️ ATTENZIONE: Caricate solo ${gasStations.size} su $totalCount stazioni!")
                    Log.w("MainActivity", "Probabilmente c'è un limite di paginazione")
                }

                if (gasStations.isNotEmpty()) {
                    // Stampa informazioni geografiche per debug
                    val latitudes = gasStations.map { it.latitude }
                    val longitudes = gasStations.map { it.longitude }
                    Log.d("MainActivity", "Range latitudini: ${latitudes.minOrNull()} - ${latitudes.maxOrNull()}")
                    Log.d("MainActivity", "Range longitudini: ${longitudes.minOrNull()} - ${longitudes.maxOrNull()}")

                    // Conta stazioni per regione (approssimativo)
                    val northStations = gasStations.count { it.latitude > 45.0 }
                    val centralStations = gasStations.count { it.latitude in 42.0..45.0 }
                    val southStations = gasStations.count { it.latitude < 42.0 }

                    Log.d("MainActivity", "Distribuzione geografica:")
                    Log.d("MainActivity", "- Nord Italia (>45°): $northStations stazioni")
                    Log.d("MainActivity", "- Centro Italia (42°-45°): $centralStations stazioni")
                    Log.d("MainActivity", "- Sud Italia (<42°): $southStations stazioni")

                    mapComponent.addGasStationMarkers(gasStations)
                    Log.d("MainActivity", "Marker aggiunti con successo alla mappa")
                } else {
                    Log.w("MainActivity", "Nessuna stazione trovata nel database")
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Errore nel caricamento delle stazioni: ${e.message}", e)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapComponent.initialize()
    }

    private fun requestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.INTERNET)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_NETWORK_STATE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }

            if (allGranted) {
                mapComponent.initialize()
                Log.d("MainActivity", "Tutti i permessi concessi, mappa inizializzata")
            } else {
                Log.w("MainActivity", "Alcuni permessi non sono stati concessi")
                // Potresti voler mostrare un messaggio all'utente qui
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapComponent.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapComponent.onPause()
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 123
    }
}