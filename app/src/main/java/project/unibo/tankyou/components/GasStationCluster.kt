package project.unibo.tankyou.components

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import org.osmdroid.bonuspack.clustering.StaticCluster
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.core.graphics.createBitmap
import project.unibo.tankyou.utils.Constants
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable

/**
 * Custom cluster implementation for gas station markers on the map.
 *
 * This class extends RadiusMarkerClusterer to provide visually appealing cluster markers
 * with different colors based on cluster size and optimized icon (LRU) caching for better performance.
 *
 * @param context The Android context used for creating drawables and accessing resources
 *
 * @author Marco Marrelli, Margherita Zanchini
 */
class GasStationCluster(context: Context) : RadiusMarkerClusterer(context) {
    /**
     * LRU cache for storing cluster icons to avoid recreating identical icons.
     * Maximum size of 50 entries should be sufficient for most use cases.
     */
    private val iconCache = LruCache<String, Drawable>(Constants.Cluster.CACHE_SIZE)

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
        val color = getColorForClusterSize(clusterSize)
        val cacheKey = "${clusterSize}_${color}"

        /**
         * Icon from cache, or create a new one if not found.
         */
        val icon = iconCache.get(cacheKey) ?: run {
            val newIcon = createClusterIcon(clusterSize, color)
            iconCache.put(cacheKey, newIcon)
            newIcon
        }

        marker.icon = icon
        return marker
    }

    /**
     * Determines the appropriate color for a cluster based on its size.
     *
     * Color scheme:
     * - Red: More than 100 gas stations (high density areas)
     * - Orange: 50-100 gas stations (medium density areas)
     * - Green: Less than 50 gas stations (low density areas)
     *
     * @param size The number of gas stations in the cluster
     *
     * @return An integer color value representing the appropriate color for the cluster
     */
    private fun getColorForClusterSize(size: Int): Int {
        return when {
            size > 100 -> Color.RED
            size >= 50 -> Color.rgb(255, 165, 0)
            else -> Color.GREEN
        }
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
     * @return A Drawable object representing the cluster icon
     */
    private fun createClusterIcon(clusterSize: Int, baseColor: Int): Drawable {
        val size = 150
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 2f - 3f

        // ✅ Crea il gradiente radiale (da colorato interno a trasparente esterno)
        val radialGradient = RadialGradient(
            centerX, centerY, // Centro del gradiente
            radius, // Raggio del gradiente
            intArrayOf(
                baseColor, // Colore interno (opaco)
                Color.argb(150, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)), // Colore intermedio (semi-trasparente)
                Color.TRANSPARENT // Colore esterno (trasparente)
            ),
            floatArrayOf(0.0f, 0.7f, 1.0f), // Posizioni dei colori (0% = centro, 70% = quasi bordo, 100% = bordo)
            Shader.TileMode.CLAMP
        )

        val paint = Paint().apply {
            isAntiAlias = true
            shader = radialGradient // ✅ Applica il gradiente invece del colore solido
            style = Paint.Style.FILL
        }

        // ✅ Bordo più sottile o opzionale
        val strokePaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(100, 255, 255, 255) // Bordo bianco semi-trasparente
            style = Paint.Style.STROKE
            strokeWidth = 1f // Ridotto da 2f
        }

        // ✅ Testo migliorato con dimensione dinamica
        val dynamicTextSize = when {
            clusterSize >= 1000 -> 20f
            clusterSize >= 100 -> 18f
            clusterSize >= 10 -> 16f
            else -> 14f
        }

        // Testo con contorno per migliore leggibilità
        val textOutlinePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = dynamicTextSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = dynamicTextSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
        }

        // Disegna il cerchio con gradiente
        canvas.drawCircle(centerX, centerY, radius, paint)

        // Disegna il bordo (opzionale)
        canvas.drawCircle(centerX, centerY, radius, strokePaint)

        // Disegna il testo
        val text = formatClusterText(clusterSize)
        val textY = centerY + (textPaint.textSize / 3f)

        // Prima il contorno del testo
        canvas.drawText(text, centerX, textY, textOutlinePaint)
        // Poi il testo principale
        canvas.drawText(text, centerX, textY, textPaint)

        return BitmapDrawable(null, bitmap)
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