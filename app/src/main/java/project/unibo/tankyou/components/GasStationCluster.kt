package project.unibo.tankyou.components

import android.content.Context
import android.graphics.*
import android.util.LruCache
import org.osmdroid.bonuspack.clustering.StaticCluster
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.core.graphics.createBitmap

class GasStationCluster(context: Context) : RadiusMarkerClusterer(context) {

    private val iconCache = LruCache<String, android.graphics.drawable.Drawable>(50)

    override fun buildClusterMarker(cluster: StaticCluster, mapView: MapView): Marker {
        val marker = super.buildClusterMarker(cluster, mapView)

        val clusterSize = cluster.size
        val color = getColorForClusterSize(clusterSize)
        val cacheKey = "${clusterSize}_${color}"

        val icon = iconCache.get(cacheKey) ?: run {
            val newIcon = createClusterIcon(clusterSize, color)
            iconCache.put(cacheKey, newIcon)
            newIcon
        }

        marker.icon = icon
        return marker
    }

    private fun getColorForClusterSize(size: Int): Int {
        return when {
            size > 100 -> Color.RED
            size >= 50 -> Color.rgb(255, 165, 0)
            else -> Color.GREEN
        }
    }

    private fun createClusterIcon(clusterSize: Int, baseColor: Int): android.graphics.drawable.Drawable {
        val size = 150
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 2f - 3f

        val paint = Paint().apply {
            isAntiAlias = true
            color = baseColor
            style = Paint.Style.FILL
        }

        val strokePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        canvas.drawCircle(centerX, centerY, radius, paint)
        canvas.drawCircle(centerX, centerY, radius, strokePaint)

        val text = formatClusterText(clusterSize)
        val textY = centerY + (textPaint.textSize / 3f)
        canvas.drawText(text, centerX, textY, textPaint)

        return android.graphics.drawable.BitmapDrawable(null, bitmap)
    }c

    private fun formatClusterText(clusterSize: Int): String {
        return when {
            clusterSize >= 1000 -> "${clusterSize / 1000}k+"
            clusterSize >= 100 -> "${clusterSize / 100}h+"
            else -> clusterSize.toString()
        }
    }
}