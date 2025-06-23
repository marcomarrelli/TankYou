package project.unibo.tankyou.utils

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import project.unibo.tankyou.R
import project.unibo.tankyou.data.database.entities.FuelType
import project.unibo.tankyou.data.database.entities.GasStationFlag
import project.unibo.tankyou.data.database.entities.GasStationType
import project.unibo.tankyou.data.repositories.AppRepository
import project.unibo.tankyou.data.repositories.AuthRepository
import project.unibo.tankyou.data.repositories.UserRepository
import project.unibo.tankyou.utils.Constants.App.APP_REPOSITORY
import project.unibo.tankyou.utils.Constants.App.AUTH_REPOSITORY
import project.unibo.tankyou.utils.Constants.App.LOG_TAG
import project.unibo.tankyou.utils.Constants.App.PERMISSIONS_LIST
import project.unibo.tankyou.utils.Constants.App.USER_REPOSITORY
import project.unibo.tankyou.utils.Constants.AppLanguage.ENGLISH
import project.unibo.tankyou.utils.Constants.AppLanguage.ITALIAN
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
import project.unibo.tankyou.utils.Constants.Map.GAS_STATION_SIZE
import project.unibo.tankyou.utils.Constants.Map.MAX_ZOOM_LEVEL
import project.unibo.tankyou.utils.Constants.Map.MIN_ZOOM_LEVEL

/**
 * Constants Global Object
 *
 * @param Map Map Related Constants
 * @param App Application Related Constants
 * @param AppLanguage Language Related Constants and Methods
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
     * @param GAS_STATION_SIZE Gas Station Map Drawing Size (Diameter, pixels)
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

        /** Gas Station Map Drawing Size (Diameter, pixels) */
        const val GAS_STATION_SIZE: Int = 112

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
            const val CLUSTER_SIZE: Int = 160

            /** Cluster Group Size (Radius, pixels) */
            const val CLUSTER_GROUP_RADIUS: Int = 320

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
     * @param APP_REPOSITORY App Repository Instance
     * @param AUTH_REPOSITORY Auth Repository Instance
     * @param USER_REPOSITORY User Repository Instance
     * @param PERMISSIONS_LIST Mandatory App Permissions
     * @param LOG_TAG Log Tag + <File>
     */
    object App {
        /** App Repository Instance */
        val APP_REPOSITORY: AppRepository = AppRepository.getInstance()

        /** Auth Repository Instance */
        val AUTH_REPOSITORY: AuthRepository = AuthRepository.getInstance()

        /** User Repository Instance */
        val USER_REPOSITORY: UserRepository = UserRepository.getInstance()

        /**
         * Mandatory App Permissions
         *
         * @param Manifest.permission.ACCESS_FINE_LOCATION Fine Location Permission
         * @param Manifest.permission.ACCESS_COARSE_LOCATION Coarse Location Permission
         * @param Manifest.permission.INTERNET Internet Permission
         * @param Manifest.permission.ACCESS_NETWORK_STATE Network State Permission
         * @param Manifest.permission.WRITE_EXTERNAL_STORAGE Write External Storage Permission
         */
        val PERMISSIONS_LIST: Array<String> = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        const val LOG_TAG = "TankYou"

        val STATUS_BAR_PADDING: Dp = 52.dp
    }

    /**
     * App Language Global Enumeration
     *
     * @param code Language Code
     *
     * @see ITALIAN Italian Language Setting
     * @see ENGLISH English Language Setting
     * @see getDisplayName Returns the display name of the language
     */
    enum class AppLanguage(val code: String) {
        /** Italian Language Setting */
        ITALIAN("it"),

        /** English Language Setting */
        ENGLISH("en");

        /**
         * Returns the display name of the language
         * across the whole application.
         */
        @Composable
        fun getDisplayName(): String {
            return when (this) {
                ITALIAN -> getResourceString(R.string.it_lang)
                ENGLISH -> getResourceString(R.string.en_lang)
            }
        }
    }

    /** Fuel Types Global List, Retrieved from Database at Startup */
    var FUEL_TYPES: List<FuelType> = emptyList()
        private set

    /** Gas Station Types Global List, Retrieved from Database at Startup */
    var GAS_STATION_TYPES: List<GasStationType> = emptyList()
        private set

    /** Gas Station Flags Global List, Retrieved from Database at Startup */
    var GAS_STATION_FLAGS: List<GasStationFlag> = emptyList()
        private set

    /** If Constants are Fully Initialized */
    private var IS_INITIALIZED = false

    /**
     * Initializes database constants asynchronously in the background.
     * This is a convenience method for calling from UI contexts.
     */
    fun initializeConstantLists() {
        /**
         * Initializes database-dependent constants by loading data from the database.
         *
         * This method MUST be called once during application startup.
         */
        suspend fun initConstLists() {
            if (IS_INITIALIZED) return

            try {
                FUEL_TYPES = APP_REPOSITORY.getFuelTypes()
                GAS_STATION_FLAGS = APP_REPOSITORY.getFlags()
                GAS_STATION_TYPES = APP_REPOSITORY.getGasStationTypes()

                IS_INITIALIZED = true
            } catch (e: Exception) {
                e.printStackTrace()

                FUEL_TYPES = emptyList()
                GAS_STATION_FLAGS = emptyList()
                GAS_STATION_TYPES = emptyList()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            initConstLists()
        }
    }
}