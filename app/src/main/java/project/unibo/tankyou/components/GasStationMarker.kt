package project.unibo.tankyou.components

import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import project.unibo.tankyou.data.database.entities.GasStation

class GasStationMarker(mapView: MapView, val gasStation: GasStation) : Marker(mapView) {
    init {
        position = GeoPoint(gasStation.latitude, gasStation.longitude)
        title = gasStation.name
        snippet = "${gasStation.address}, ${gasStation.city}, ${gasStation.province}"

        // icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_gas_station)
    }

    override fun getPosition(): GeoPoint {
        return GeoPoint(gasStation.latitude, gasStation.longitude)
    }
}