package project.unibo.tankyou.data.repositories

import android.util.LruCache
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import org.osmdroid.util.BoundingBox
import project.unibo.tankyou.data.DatabaseClient
import project.unibo.tankyou.data.database.entities.Fuel
import project.unibo.tankyou.data.database.entities.FuelType
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.database.entities.GasStationFlag
import project.unibo.tankyou.data.database.entities.GasStationType

/**
 * Repository class that handles data access for [GasStation]s and fuel information.
 *
 * Implements singleton pattern and provides caching functionality for improved performance.
 */
class AppRepository {
    private val client = DatabaseClient.client
    private val stationCache = LruCache<String, List<GasStation>>(50)

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        /**
         * Returns the singleton instance of AppRepository.
         *
         * @return the singleton [AppRepository] instance
         */
        fun getInstance(): AppRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository().also { INSTANCE = it }
            }
        }
    }

    /**
     * Retrieves all [GasStation]s from the database using batch processing.
     *
     * @return list of all [GasStation]s
     */
    suspend fun getAllStations(): List<GasStation> {
        val allStations = mutableListOf<GasStation>()
        var lastId = 0L
        val batchSize = 1000

        try {
            do {
                val batch = client.from("gas_stations")
                    .select {
                        filter { gt("id", lastId) }
                        order("id", Order.ASCENDING)
                        limit(batchSize.toLong())
                    }
                    .decodeList<GasStation>()

                allStations.addAll(batch)

                if (batch.isNotEmpty()) {
                    lastId = batch.last().id.toLong()
                }
            } while (batch.size == batchSize)
        } catch (e: Exception) {
            e
        }

        return allStations
    }

    /**
     * Retrieves [GasStation]s within specified geographical bounds with caching support.
     *
     * @param minLat minimum latitude boundary
     * @param maxLat maximum latitude boundary
     * @param minLon minimum longitude boundary
     * @param maxLon maximum longitude boundary
     *
     * @return list of [GasStation]s within the specified bounds
     */
    suspend fun getStationsInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<GasStation> {
        val cacheKey = "${minLat}_${maxLat}_${minLon}_${maxLon}"

        stationCache.get(cacheKey)?.let { return it }

        return try {
            val stations = client.from("gas_stations")
                .select {
                    filter {
                        and {
                            gte("latitude", minLat)
                            lte("latitude", maxLat)
                            gte("longitude", minLon)
                            lte("longitude", maxLon)
                        }
                    }
                    limit(1000)
                }
                .decodeList<GasStation>()

            stationCache.put(cacheKey, stations)
            stations

        } catch (e: Exception) {
            e
            emptyList()
        }
    }

    /**
     * Retrieves [GasStation]s optimized for specific zoom levels with adaptive limits.
     *
     * @param bounds the geographical bounding box
     * @param zoomLevel the map zoom level for optimization
     *
     * @return list of [GasStation]s optimized for the zoom level
     */
    suspend fun getStationsForZoomLevel(
        bounds: BoundingBox,
        zoomLevel: Double
    ): List<GasStation> {
        val roundedZoom = kotlin.math.round(zoomLevel * 2) / 2.0
        val cacheKey =
            "${bounds.latSouth}_${bounds.latNorth}_${bounds.lonWest}_${bounds.lonEast}_$roundedZoom"

        stationCache.get(cacheKey)?.let {
            return it
        }

        val limit = when {
            zoomLevel < 8 -> 10000
            zoomLevel < 10 -> 5000
            zoomLevel < 13 -> 1000
            zoomLevel < 16 -> 250
            else -> 50
        }

        return try {
            val stations = client.from("gas_stations")
                .select {
                    filter {
                        and {
                            gte("latitude", bounds.latSouth)
                            lte("latitude", bounds.latNorth)
                            gte("longitude", bounds.lonWest)
                            lte("longitude", bounds.lonEast)
                        }
                    }
                    limit(limit.toLong())
                    order("id", Order.ASCENDING)
                }
                .decodeList<GasStation>()

            stationCache.put(cacheKey, stations)
            stations
        } catch (e: Exception) {
            e
            emptyList()
        }
    }

    /**
     * Retrieves [GasStation]s filtered by specific provinces.
     *
     * @param provinces list of province names to filter by
     *
     * @return list of [GasStation]s in the specified provinces
     */
    suspend fun getStationsByProvinces(provinces: List<String>): List<GasStation> {
        return client.from("gas_stations")
            .select {
                filter {
                    isIn("province", provinces)
                }
            }
            .decodeList<GasStation>()
    }

    /**
     * Retrieves a specific [GasStation] by its ID.
     *
     * @param id the unique identifier of the [GasStation]
     * @return the [GasStation] if found, null otherwise
     */
    suspend fun getStationById(id: String): GasStation? {
        return client.from("gas_stations").select {
            filter { eq("id", id) }
        }.decodeSingleOrNull<GasStation>()
    }

    /**
     * Retrieves the total count of [GasStation]s in the database.
     *
     * @return the total number of [GasStation]s
     */
    suspend fun getStationCount(): Long {
        return client.from("gas_stations").select().countOrNull() ?: 0L
    }

    /**
     * Retrieves all available [Fuel] types from the database.
     *
     * @return list of all [Fuel] types
     */
    suspend fun getAllFuelTypes(): List<Fuel> {
        return client.from("fuels").select().decodeList<Fuel>()
    }

    /**
     * Retrieves a specific [Fuel] type by its ID.
     *
     * @param id the unique identifier of the [Fuel] type
     *
     * @return the [Fuel] type if found, null otherwise
     */
    suspend fun getFuelById(id: String): Fuel? {
        return client.from("fuels").select {
            filter { eq("id", id) }
        }.decodeSingleOrNull<Fuel>()
    }

    /**
     * Retrieves fuel prices for a specific gas station
     * @param stationId The ID of the gas station
     * @return List of fuel prices for the station
     */
    suspend fun getFuelPricesForStation(stationId: Long): List<Fuel> {
        return try {
            client.from("fuels")
                .select {
                    filter { eq("station_id", stationId) }
                    order("type", Order.ASCENDING)
                }
                .decodeList<Fuel>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getFuelTypes(): List<FuelType> {
        return try {
            client.from("fuel_types")
                .select()
                .decodeList<FuelType>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getGasStationTypes(): List<GasStationType> {
        return try {
            client.from("gas_station_types")
                .select()
                .decodeList<GasStationType>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getFlags(): List<GasStationFlag> {
        return try {
            client.from("gas_station_flags")
                .select()
                .decodeList<GasStationFlag>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Searches for gas stations based on a query string.
     * @param query The search query
     * @return List of matching gas stations
     */
    suspend fun searchStations(query: String): List<GasStation> {
        if (query.isBlank()) return emptyList()

        val searchQuery = "%${query.lowercase()}%"

        return try {
            client.from("gas_stations")
                .select {
                    filter {
                        or {
                            ilike("name", searchQuery)
                            ilike("city", searchQuery)
                            ilike("province", searchQuery)
                        }
                    }
                }
                .decodeList<GasStation>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Clears all cached data from the station cache.
     */
    fun clearCache() {
        stationCache.evictAll()
    }
}