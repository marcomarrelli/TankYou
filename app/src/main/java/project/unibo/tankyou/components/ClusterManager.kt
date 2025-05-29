package project.unibo.tankyou.components

import android.content.Context
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import project.unibo.tankyou.data.database.entities.GasStation
import kotlin.math.*

data class GasStationCluster(
    val centerLat: Double,
    val centerLon: Double,
    val stations: MutableList<GasStation> = mutableListOf(),
    var marker: Marker? = null
) {
    fun addStation(station: GasStation) {
        stations.add(station)
    }

    fun getCenter(): GeoPoint = GeoPoint(centerLat, centerLon)

    fun size(): Int = stations.size
}

class ClusterManager(
    private val context: Context,
    private val mapView: MapView
) {
    private val clusters = mutableListOf<GasStationCluster>()
    private val allStations = mutableListOf<GasStation>()
    private var currentZoomLevel = 7.0

    // Distanza minima tra cluster in base al livello di zoom
    private fun getClusterDistance(zoomLevel: Double): Double {
        return when {
            zoomLevel <= 8 -> 0.5    // ~50km
            zoomLevel <= 10 -> 0.2   // ~20km
            zoomLevel <= 12 -> 0.1   // ~10km
            zoomLevel <= 14 -> 0.05  // ~5km
            zoomLevel <= 16 -> 0.02  // ~2km
            else -> 0.01             // ~1km
        }
    }

    fun setStations(stations: List<GasStation>) {
        allStations.clear()
        allStations.addAll(stations)
        updateClusters()
    }

    fun updateClusters() {
        val zoomLevel = mapView.zoomLevelDouble
        currentZoomLevel = zoomLevel

        // Rimuovi i marker esistenti
        clearMarkers()

        // Ricrea i cluster in base al nuovo zoom
        createClusters(zoomLevel)

        // Aggiungi i nuovi marker
        addMarkersToMap()
    }

    private fun createClusters(zoomLevel: Double) {
        clusters.clear()
        val clusterDistance = getClusterDistance(zoomLevel)

        for (station in allStations) {
            var addedToCluster = false

            // Cerca un cluster esistente vicino
            for (cluster in clusters) {
                val distance = calculateDistance(
                    station.latitude, station.longitude,
                    cluster.centerLat, cluster.centerLon
                )

                if (distance <= clusterDistance) {
                    cluster.addStation(station)
                    addedToCluster = true
                    break
                }
            }

            // Se non trovato, crea un nuovo cluster
            if (!addedToCluster) {
                val newCluster = GasStationCluster(station.latitude, station.longitude)
                newCluster.addStation(station)
                clusters.add(newCluster)
            }
        }
    }

    private fun addMarkersToMap() {
        for (cluster in clusters) {
            val marker = Marker(mapView)
            marker.position = cluster.getCenter()

            if (cluster.size() == 1) {
                // Marker singolo
                val station = cluster.stations.first()
                marker.title = station.name
                marker.snippet = "${station.address}, ${station.city}"
            } else {
                // Marker cluster
                marker.title = "${cluster.size()} stazioni"
                marker.snippet = "Zoom per vedere le singole stazioni"

                // Icona personalizzata per cluster (opzionale)
                marker.icon = createClusterIcon(cluster.size())
            }

            // Listener per click sui cluster
            marker.setOnMarkerClickListener { clickedMarker, _ ->
                if (cluster.size() > 1 && currentZoomLevel < 16) {
                    // Zoom sul cluster se ha più stazioni e non siamo già molto vicini
                    mapView.controller.animateTo(cluster.getCenter())
                    mapView.controller.setZoom(currentZoomLevel + 2)
                    true
                } else if (cluster.size() > 1) {
                    // Mostra lista delle stazioni nel cluster
                    showClusterStationsList(cluster)
                    true
                } else {
                    false // Lascia che il marker gestisca il click normalmente
                }
            }

            cluster.marker = marker
            mapView.overlays.add(marker)
        }

        mapView.invalidate()
    }

    private fun createClusterIcon(count: Int): android.graphics.drawable.Drawable? {
        // Crea un'icona personalizzata per i cluster
        // Per ora ritorna null per usare l'icona di default
        return null
    }

    private fun showClusterStationsList(cluster: GasStationCluster) {
        // Mostra un dialog con la lista delle stazioni nel cluster
        val stationNames = cluster.stations.joinToString("\n") {
            "${it.name} - ${it.address}"
        }

        android.app.AlertDialog.Builder(context)
            .setTitle("${cluster.size()} stazioni in questa area")
            .setMessage(stationNames)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun clearMarkers() {
        val markersToRemove = mapView.overlays.filterIsInstance<Marker>()
        mapView.overlays.removeAll(markersToRemove)
    }

    // Calcola la distanza tra due punti in gradi
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return sqrt((lat2 - lat1).pow(2) + (lon2 - lon1).pow(2))
    }
}