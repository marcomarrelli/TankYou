package project.unibo.tankyou.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import project.unibo.tankyou.R
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.ui.theme.ThemeManager
import project.unibo.tankyou.utils.Constants

/**
 * Custom marker for displaying gas stations on the map.
 *
 * @property mapView The MapView to which this marker is added.
 * @property gasStation The GasStation data to display.
 * @property context The application context.
 */
class GasStationMarker(
    mapView: MapView,
    val gasStation: GasStation,
    private val context: Context
) : Marker(mapView) {

    /**
     * Initializes the marker with its position, title, snippet, icon, and anchor point.
     */
    init {
        position = GeoPoint(gasStation.latitude, gasStation.longitude)
        title = gasStation.name
        snippet = "${gasStation.address}, ${gasStation.city}, ${gasStation.province}"

        icon = createGasStationIcon()
        setAnchor(ANCHOR_CENTER, ANCHOR_BOTTOM)
    }

    /**
     * Creates a custom icon for the gas station marker.
     *
     * @return A BitmapDrawable representing the custom icon.
     */
    private fun createGasStationIcon(): BitmapDrawable {
        val size = Constants.Map.GAS_STATION_SIZE
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            isAntiAlias = true
            color = ThemeManager.palette.primary.toArgb()
            style = Paint.Style.FILL
        }

        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 2f - 4f

        canvas.drawCircle(centerX, centerY, radius, paint)

        val iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_gas_station)
        if (iconDrawable != null) {
            val iconSize = (size * 0.5f).toInt()
            val left = (size - iconSize) / 2
            val top = (size - iconSize) / 2

            iconDrawable.setBounds(left, top, left + iconSize, top + iconSize)
            iconDrawable.setTint(ThemeManager.palette.black.toArgb())
            iconDrawable.draw(canvas)
        }

        return bitmap.toDrawable(context.resources)
    }

    /**
     * Retrieves the geographical position of the gas station.
     *
     * @return A GeoPoint representing the latitude and longitude of the gas station.
     */
    override fun getPosition(): GeoPoint {
        return GeoPoint(gasStation.latitude, gasStation.longitude)
    }
}