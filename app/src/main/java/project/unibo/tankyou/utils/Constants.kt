package project.unibo.tankyou.utils

import android.Manifest
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import project.unibo.tankyou.data.repositories.AppRepository

/**
 * Constants Global Object
 */
object Constants {
    /**
     * Map Constants Global Object
     */
    object Map {
        /** Minimum Zoom Level */
        const val MIN_ZOOM_LEVEL: Double = 7.0

        /** Default Zoom Level */
        const val DEFAULT_ZOOM_LEVEL: Double = 10.0

        /** Maximum Zoom Level */
        const val MAX_ZOOM_LEVEL: Double = 19.0

        /** Boundary Buffer */
        const val BOUNDS_BUFFER: Double = 0.5

        /** Default Geo Point, pointing Rome */
        val DEFAULT_GEO_POINT: GeoPoint = GeoPoint(42.5, 12.5)

        /** Geo Bounds for Italy Region */
        val BOUNDS: BoundingBox = BoundingBox(
            47.5,
            19.0,
            36.0,
            6.0
        )

        /**
         * Cluster Constants Global Object
         */
        object Cluster {
            const val CLUSTER_MAX_SIZE: Int = 100

            /** Cluster Map Drawing Size (Diameter, pixels) */
            const val CLUSTER_SIZE: Int = 200

            /** Cluster Group Size (Radius, pixels) */
            const val CLUSTER_GROUP_RADIUS: Int = 250

            const val CLUSTER_TEXT_FONT_SIZE: Float = 16f
        }

        object Cache {
            /** Cluster Saved in Cache for Efficiency between Zoom Actions */
            const val CACHE_SIZE: Int = 100

            /** Map Tile Count in Cache */
            const val TILE_COUNT: Short = 12

            /** Map Tile Overshoot per Layer */
            const val TILE_OVERSHOOT: Short = 2
        }
    }

    object App {
        val REPOSITORY: AppRepository = AppRepository.getInstance()
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}