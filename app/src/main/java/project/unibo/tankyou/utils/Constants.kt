package project.unibo.tankyou.utils

import android.Manifest

import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

import project.unibo.tankyou.data.repositories.AppRepository

import project.unibo.tankyou.utils.Constants.App.PERMISSIONS
import project.unibo.tankyou.utils.Constants.App.REPOSITORY

import project.unibo.tankyou.utils.Constants.Map.BOUNDS
import project.unibo.tankyou.utils.Constants.Map.BOUNDS_BUFFER
import project.unibo.tankyou.utils.Constants.Map.Cache.CACHE_SIZE
import project.unibo.tankyou.utils.Constants.Map.Cache.TILE_COUNT
import project.unibo.tankyou.utils.Constants.Map.Cache.TILE_OVERSHOOT
import project.unibo.tankyou.utils.Constants.Map.Cluster.CLUSTER_GROUP_RADIUS
import project.unibo.tankyou.utils.Constants.Map.Cluster.CLUSTER_MAX_COUNT
import project.unibo.tankyou.utils.Constants.Map.Cluster.CLUSTER_SIZE
import project.unibo.tankyou.utils.Constants.Map.Cluster.CLUSTER_TEXT_FONT_SIZE
import project.unibo.tankyou.utils.Constants.Map.DEFAULT_GEO_POINT
import project.unibo.tankyou.utils.Constants.Map.DEFAULT_ZOOM_LEVEL
import project.unibo.tankyou.utils.Constants.Map.MAX_ZOOM_LEVEL
import project.unibo.tankyou.utils.Constants.Map.MIN_ZOOM_LEVEL

/**
 * Constants Global Object
 *
 * @param Map Map Related Constants
 * @param App Application Related Constants
 */
object Constants {
    /**
     * Map Constants Global Object
     *
     * @param MIN_ZOOM_LEVEL Minimum Zoom Level
     * @param DEFAULT_ZOOM_LEVEL Default Zoom Level
     * @param MAX_ZOOM_LEVEL Maximum Zoom Level
     * @param BOUNDS_BUFFER Boundary Buffer
     * @param DEFAULT_GEO_POINT Default Geo Point, pointing Rome
     * @param BOUNDS Geo Bounds for Italy Region
     *
     * @param Cluster Map Clusters Related Constants
     * @param Cache Map Cache Related Constants
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
        val DEFAULT_GEO_POINT: GeoPoint = GeoPoint(41.5335, 12.2858)

        /** Geo Bounds for Italy Region */
        val BOUNDS: BoundingBox = BoundingBox(
            47.5,
            19.0,
            36.0,
            6.0
        )

        /**
         * Map Cluster Constants Global Object
         *
         * @param CLUSTER_MAX_COUNT Cluster Maximum Size
         * @param CLUSTER_SIZE Cluster Map Drawing Size (Diameter, pixels)
         * @param CLUSTER_GROUP_RADIUS Cluster Group Size (Radius, pixels)
         * @param CLUSTER_TEXT_FONT_SIZE Default Font Size for Cluster Text
         */
        object Cluster {
            /** Cluster Maximum Size */
            const val CLUSTER_MAX_COUNT: Int = 100

            /** Cluster Map Drawing Size (Diameter, pixels) */
            const val CLUSTER_SIZE: Int = 175

            /** Cluster Group Size (Radius, pixels) */
            const val CLUSTER_GROUP_RADIUS: Int = 250

            /** Default Font Size for Cluster Text */
            const val CLUSTER_TEXT_FONT_SIZE: Float = 48f
        }

        /**
         * Map Cache Constants Global Object
         *
         * @param CACHE_SIZE Cluster Saved in Cache for Efficiency between Zoom Actions
         * @param TILE_COUNT Map Tile Count in Cache
         * @param TILE_OVERSHOOT Map Tile Overshoot per Layer
         */
        object Cache {
            /** Cluster Saved in Cache for Efficiency between Zoom Actions */
            const val CACHE_SIZE: Int = 100

            /** Map Tile Count in Cache */
            const val TILE_COUNT: Short = 12

            /** Map Tile Overshoot per Layer */
            const val TILE_OVERSHOOT: Short = 2
        }
    }

    /**
     * Application Constants Global Object
     *
     * @param REPOSITORY App Repository Instance
     * @param PERMISSIONS Mandatory App Permissions
     */
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