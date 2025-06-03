package project.unibo.tankyou.components

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.bonuspack.clustering.StaticCluster
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

import project.unibo.tankyou.utils.Constants

/**
 * Custom cluster implementation for gas station markers on the map.
 *
 * This class extends RadiusMarkerClusterer to provide visually appealing cluster markers
 * with different colors based on cluster size and optimized icon (LRU) caching for better performance.
 *
 * @param context The Android context used for creating drawables and accessing resources
 */
class GasStationCluster(context: Context) : RadiusMarkerClusterer(context) {
    /**
     * LRU cache for storing cluster icons to avoid recreating identical icons.
     * Maximum size of 50 entries should be sufficient for most use cases.
     */
    private val iconCache = LruCache<String, Drawable>(Constants.Map.Cache.CACHE_SIZE)

    /**
     * Determines the appropriate color for a cluster based on its size.
     *
     * Color scheme:
     * - Red: More than [Constants.Map.Cluster.CLUSTER_MAX_SIZE] gas stations (high density areas)
     * - Orange: More than half of [Constants.Map.Cluster.CLUSTER_MAX_SIZE] gas stations (medium density areas)
     * - Green: Less than half of [Constants.Map.Cluster.CLUSTER_MAX_SIZE] gas stations (low density areas)
     *
     * @param size The number of gas stations in the cluster
     *
     * @return An integer color value representing the appropriate color for the cluster
     */
    private fun getClusterColor(size: Int): Int {
        return when {
            size > Constants.Map.Cluster.CLUSTER_MAX_SIZE -> Color.RED
            size >= (Constants.Map.Cluster.CLUSTER_MAX_SIZE / 2) -> Color.rgb(255, 165, 0)
            else -> Color.GREEN
        }
    }

    /**
     * Creates a custom cluster marker with color-coded icon based on cluster size.
     *
     * This method overrides the default cluster marker creation to provide:
     * - Custom colored icons based on the number of gas stations in the cluster
     * - Efficient icon caching using LRU cache to improve performance
     * - Consistent visual appearance across similar-sized clusters
     *
     * @param cluster The StaticCluster containing the grouped markers
     * @param mapView The MapView where the cluster will be displayed
     *
     * @return A Marker object with the custom cluster icon
     */
    override fun buildClusterMarker(cluster: StaticCluster, mapView: MapView): Marker {
        val marker = super.buildClusterMarker(cluster, mapView)

        val clusterSize = cluster.size
        val color = getClusterColor(clusterSize)
        val cacheKey = "${clusterSize}_${color}"

        /**
         * Icon from cache, or create a new one if not found.
         */
        val icon = iconCache.get(cacheKey) ?: run {
            val newIcon = buildClusterIcon(clusterSize, color)
            iconCache.put(cacheKey, newIcon)
            newIcon
        }

        marker.icon = icon
        return marker
    }

    /**
     * Creates a custom circular cluster icon with the specified size and color.
     *
     * The icon consists of:
     * - A filled circle with the specified base color
     * - A white stroke border for better visibility
     * - Centered white text showing the formatted cluster size
     * - Anti-aliased rendering for smooth appearance
     *
     * @param clusterSize The number of gas stations in the cluster
     * @param baseColor The background color for the cluster icon
     *
     * @return A Drawable object representing the cluster icon
     */
    private fun buildClusterIcon(clusterSize: Int, baseColor: Int): Drawable {
        val size = Constants.Map.Cluster.CLUSTER_SIZE
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 2f - 3f

        val radialGradient = RadialGradient(
            centerX,
            centerY,
            radius,
            intArrayOf(
                baseColor,
                Color.argb(
                    255 / 2,
                    Color.red(baseColor),
                    Color.green(baseColor),
                    Color.blue(baseColor)
                ),
                Color.TRANSPARENT
            ),
            floatArrayOf(
                0.0f,
                0.5f,
                1.0f
            ),
            Shader.TileMode.CLAMP
        )

        val paint = Paint().apply {
            isAntiAlias = true
            shader = radialGradient
            style = Paint.Style.FILL
        }

        val strokePaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(100, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val dynamicTextSize = when {
            clusterSize >= 1000 -> (Constants.Map.Cluster.CLUSTER_TEXT_FONT_SIZE * 1.75)
            clusterSize >= 100 -> (Constants.Map.Cluster.CLUSTER_TEXT_FONT_SIZE * 1.5)
            clusterSize >= 10 -> (Constants.Map.Cluster.CLUSTER_TEXT_FONT_SIZE * 1.25)
            else -> Constants.Map.Cluster.CLUSTER_TEXT_FONT_SIZE
        }.toFloat()

        val textOutlinePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = dynamicTextSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = dynamicTextSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
        }

        canvas.drawCircle(centerX, centerY, radius, paint)

        canvas.drawCircle(centerX, centerY, radius, strokePaint)

        val text = formatClusterText(clusterSize)
        val textY = centerY + (textPaint.textSize / 3f)

        canvas.drawText(text, centerX, textY, textOutlinePaint)
        canvas.drawText(text, centerX, textY, textPaint)

        return bitmap.toDrawable(Resources.getSystem())
    }

    /**
     * Formats the cluster size number for display on the cluster icon.
     *
     * Formatting rules:
     * - 1000+: Display as "Xk+" (e.g., "2k+" for 2000-2999)
     * - 100-999: Display as "Xh+" (e.g., "3h+" for 300-399)
     * - 1-99: Display the exact number
     *
     * This formatting ensures the text fits well within the cluster icon
     * while providing meaningful information about cluster density.
     *
     * @param clusterSize The number of gas stations in the cluster
     *
     * @return A formatted string representation of the cluster size
     */
    private fun formatClusterText(clusterSize: Int): String {
        return when {
            clusterSize >= 1000 -> "${clusterSize / 1000}k+"
            clusterSize >= 100 -> "${clusterSize / 100}h+"
            else -> clusterSize.toString()
        }
    }
}