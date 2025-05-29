package project.unibo.tankyou

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import project.unibo.tankyou.components.MapComponent
import project.unibo.tankyou.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var mapComponent: MapComponent
    private val applicationScope = CoroutineScope(Dispatchers.Main)
    private lateinit var repository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()

        val mapContainer = findViewById<RelativeLayout>(R.id.mapContainer)
        mapComponent = MapComponent(this, mapContainer)
        repository = AppRepository.getInstance()

        applicationScope.launch {
            try {
                Log.e("[ERR]", "GAS STATION COUNT: ${repository.getStationCount()}")
            } catch (e: Exception) {
                Log.e("[ERR]", "Errore nel caricamento dati: ${e.message}")
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
            }
            else {
                // TODO: Qualcosa...
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