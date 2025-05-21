package project.unibo.tankyou

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import project.unibo.tankyou.components.MapComponent

class MainActivity : AppCompatActivity() {

    private lateinit var mapComponent: MapComponent
    private val PERMISSIONS_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()

        val mapContainer = findViewById<RelativeLayout>(R.id.mapContainer)
        mapComponent = MapComponent(this, mapContainer)
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

        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
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
            } else {
                Toast.makeText(
                    this,
                    "Alcune funzionalità potrebbero essere limitate senza i permessi richiesti",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // fun zoomToLocation(latitude: Double, longitude: Double) {
    //     mapComponent.centerOn(latitude, longitude)
    //     mapComponent.setZoom(15.0)
    // }

    // fun enableFollowMode() {
    //     mapComponent.enableLocationFollowing(true)
    // }

    // fun disableFollowMode() {
    //     mapComponent.enableLocationFollowing(false)
    // }

    override fun onResume() {
        super.onResume()
        mapComponent.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapComponent.onPause()
    }
}