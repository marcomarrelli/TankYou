package project.unibo.tankyou.utils

import android.Manifest

/**
 * Constants Global Object
 *
 * @param Cluster Cluster Constants
 *
 * @author TankYou Project
 */
object Constants {
    /**
     * Cluster Constants Global Object
     *
     * @param CLUSTER_SIZE Cluster Map Drawing Size (Diameter, pixels)
     * @param CLUSTER_GROUP_RADIUS Cluster Group Size (Radius, pixels)
     * @param CACHE_SIZE Cluster Saved in Cache for Efficiency between Zoom Actions
     *
     * @author TankYou Project
     */
    object Cluster {
        /** Cluster Map Drawing Size (Diameter, pixels) */
        const val CLUSTER_SIZE: Int = 200

        /** Cluster Group Size (Radius, pixels) */
        const val CLUSTER_GROUP_RADIUS: Int = 250

        /** Cluster Saved in Cache for Efficiency between Zoom Actions */
        const val CACHE_SIZE: Int = 100
    }

    val APP_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
}