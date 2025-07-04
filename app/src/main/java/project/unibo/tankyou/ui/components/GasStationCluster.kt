package project.unibo.tankyou.ui.components

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.bonuspack.clustering.StaticCluster
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import project.unibo.tankyou.ui.theme.ThemeManager
import project.unibo.tankyou.ui.theme.toAndroidColor
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
    private val iconCache: LruCache<String, Drawable> =
        LruCache<String, Drawable>(Constants.Map.Cache.CACHE_SIZE)

    /**
     * Determines the appropriate color for a cluster based on its size.
     *
     * Color scheme:
     * - Alert: More than [Constants.Map.Cluster.CLUSTER_MAX_COUNT] gas stations (high density areas)
     * - Warning: More than half of [Constants.Map.Cluster.CLUSTER_MAX_COUNT] gas stations (medium density areas)
     * - OK: Less than half of [Constants.Map.Cluster.CLUSTER_MAX_COUNT] gas stations (low density areas)
     *
     * @param size The number of gas stations in the cluster
     *
     * @return An integer color value representing the appropriate color for the cluster
     */
    private fun getClusterColor(size: Int): Int {
        Log.d(Constants.App.LOG_TAG, "Determining cluster color for size: $size")

        val color: Int = when {
            size > Constants.Map.Cluster.CLUSTER_MAX_COUNT -> ThemeManager.palette.alert.toAndroidColor()
            size >= (Constants.Map.Cluster.CLUSTER_MAX_COUNT / 2) -> ThemeManager.palette.warning.toAndroidColor()
            else -> ThemeManager.palette.ok.toAndroidColor()
        }

        Log.d(Constants.App.LOG_TAG, "Selected cluster color: $color for size: $size")
        return color
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
     * @return A [Marker] object with the custom cluster icon
     */
    override fun buildClusterMarker(cluster: StaticCluster, mapView: MapView): Marker {
        Log.d(
            Constants.App.LOG_TAG,
            "Building cluster marker for cluster with ${cluster.size} items"
        )

        val marker = super.buildClusterMarker(cluster, mapView)

        val clusterSize: Int = cluster.size
        val color: Int = getClusterColor(clusterSize)
        val cacheKey = "${clusterSize}_${color}"

        /**
         * Icon from cache, or create a new one if not found.
         */
        val icon: Drawable = iconCache.get(cacheKey) ?: run {
            Log.d(Constants.App.LOG_TAG, "Creating new cluster icon for cache key: $cacheKey")
            val newIcon: Drawable = buildClusterIcon(clusterSize, color)
            iconCache.put(cacheKey, newIcon)
            Log.d(Constants.App.LOG_TAG, "Cached new cluster icon. Cache size: ${iconCache.size()}")
            newIcon
        }

        marker.setOnMarkerClickListener { marker, _ ->
            Log.d(Constants.App.LOG_TAG, "Cluster marker clicked, zooming to bounds")
            // the last parameter is the zoom border padding
            mapView.zoomToBoundingBox(
                marker.bounds,
                true,
                Constants.Map.Cluster.CLUSTER_SIZE / 4
            )
            true
        }

        marker.icon = icon
        Log.d(Constants.App.LOG_TAG, "Successfully built cluster marker")
        return marker
    }

    /**
     * Creates a custom circular cluster icon with the specified size and color.
     *
     * The icon consists of:
     * - A filled circle with a smooth radial gradient
     * - Centered text with high contrast and outline for better visibility
     * - Anti-aliased rendering for smooth appearance
     *
     * @param clusterSize The number of gas stations in the cluster
     * @param baseColor The background color for the cluster icon
     *
     * @return A [Drawable] object representing the cluster icon
     */
    private fun buildClusterIcon(clusterSize: Int, baseColor: Int): Drawable {
        Log.d(
            Constants.App.LOG_TAG,
            "Building cluster icon for size: $clusterSize with color: $baseColor"
        )

        val size: Int = Constants.Map.Cluster.CLUSTER_SIZE
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val centerX: Float = size / 2f
        val centerY: Float = size / 2f
        val radius: Float = size / 2f - 2f

        val radialGradient = RadialGradient(
            centerX,
            centerY,
            radius,
            intArrayOf(
                baseColor,
                Color.argb(
                    125,
                    Color.red(baseColor),
                    Color.green(baseColor),
                    Color.blue(baseColor)
                ),
                Color.argb(
                    25,
                    Color.red(baseColor),
                    Color.green(baseColor),
                    Color.blue(baseColor)
                )
            ),
            floatArrayOf(
                0.0f,
                0.7f,
                1.0f
            ),
            Shader.TileMode.CLAMP
        )

        val paint = Paint().apply {
            isAntiAlias = true
            shader = radialGradient
            style = Paint.Style.FILL
        }

        val dynamicTextSize: Float = when {
            clusterSize >= 1000 -> (Constants.Map.Cluster.CLUSTER_TEXT_FONT_SIZE * 0.8)
            clusterSize >= 100 -> Constants.Map.Cluster.CLUSTER_TEXT_FONT_SIZE
            clusterSize >= 10 -> (Constants.Map.Cluster.CLUSTER_TEXT_FONT_SIZE * 1.2)
            else -> Constants.Map.Cluster.CLUSTER_TEXT_FONT_SIZE * 1.4
        }.toFloat()

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = ThemeManager.palette.black.toAndroidColor()
            textSize = dynamicTextSize
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
            strokeWidth = 5f
            typeface = Typeface.DEFAULT_BOLD
        }

        canvas.drawCircle(centerX, centerY, radius, paint)

        val text: String = formatClusterText(clusterSize)
        val textY: Float = centerY + (textPaint.textSize / 3f)

        canvas.drawText(text, centerX, textY, textPaint)

        Log.d(Constants.App.LOG_TAG, "Successfully created cluster icon for size: $clusterSize")
        return bitmap.toDrawable(Resources.getSystem())
    }

    /**
     * Formats the cluster size number for display on the cluster icon.
     *
     * Formatting rules:
     * - 1000+: Display as "XK+"
     * - 100-999: Display as "XH+"
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
        val formattedText: String = when {
            clusterSize >= 1000 -> "${clusterSize / 1000}K+"
            clusterSize >= 100 -> "${clusterSize / 100}H+"
            else -> clusterSize.toString()
        }

        Log.d(
            Constants.App.LOG_TAG,
            "Formatted cluster text: '$formattedText' for size: $clusterSize"
        )
        return formattedText
    }
}