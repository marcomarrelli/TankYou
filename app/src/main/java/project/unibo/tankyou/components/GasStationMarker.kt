package project.unibo.tankyou.components

import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

import project.unibo.tankyou.data.database.entities.GasStation

/**
 * Custom marker implementation for gas station locations on the map.
 *
 * @param mapView the MapView where the marker will be displayed
 * @param gasStation the GasStation entity containing location and details
 */
class GasStationMarker(mapView: MapView, val gasStation: GasStation) : Marker(mapView) {
    init {
        position = GeoPoint(gasStation.latitude, gasStation.longitude)
        title = gasStation.name
        snippet = "${gasStation.address}, ${gasStation.city}, ${gasStation.province}"
    }

    /**
     * Gets the geographical position of the gas station.
     *
     * @return a GeoPoint representing the gas station's coordinates
     */
    override fun getPosition(): GeoPoint {
        return GeoPoint(gasStation.latitude, gasStation.longitude)
    }
}