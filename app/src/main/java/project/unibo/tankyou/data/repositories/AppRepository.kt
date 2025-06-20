package project.unibo.tankyou.data.repositories

import android.util.LruCache
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns.Companion.raw
import io.github.jan.supabase.postgrest.query.Order
import org.osmdroid.util.BoundingBox
import project.unibo.tankyou.data.DatabaseClient
import project.unibo.tankyou.data.database.entities.Fuel
import project.unibo.tankyou.data.database.entities.FuelType
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.database.entities.GasStationFlag
import project.unibo.tankyou.data.database.entities.GasStationType

data class SearchFilters(
    val flags: List<GasStationFlag> = emptyList(),
    val fuelTypes: List<FuelType> = emptyList(),
    val serviceTypes: List<Boolean> = emptyList()
)

class AppRepository {
    private val client = DatabaseClient.client
    private val stationCache = LruCache<String, List<GasStation>>(50)

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(): AppRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository().also { INSTANCE = it }
            }
        }
    }

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

    suspend fun getStationsByProvinces(provinces: List<String>): List<GasStation> {
        return client.from("gas_stations")
            .select {
                filter {
                    isIn("province", provinces)
                }
            }
            .decodeList<GasStation>()
    }

    suspend fun getStationById(id: String): GasStation? {
        return client.from("gas_stations").select {
            filter { eq("id", id) }
        }.decodeSingleOrNull<GasStation>()
    }

    suspend fun getStationCount(): Long {
        return client.from("gas_stations").select().countOrNull() ?: 0L
    }

    suspend fun getAllFuelTypes(): List<Fuel> {
        return client.from("fuels").select().decodeList<Fuel>()
    }

    suspend fun getFuelById(id: String): Fuel? {
        return client.from("fuels").select {
            filter { eq("id", id) }
        }.decodeSingleOrNull<Fuel>()
    }

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

    suspend fun searchStationsWithFilters(
        query: String,
        filters: SearchFilters
    ): List<GasStation> {
        if (query.isBlank() && filters.flags.isEmpty() && filters.fuelTypes.isEmpty() && filters.serviceTypes.isEmpty()) {
            return emptyList()
        }

        return try {
            val conditions = mutableListOf<String>()

            if (query.isNotBlank()) {
                val searchQuery = "%${query.lowercase()}%"
                conditions.add("(name.ilike.$searchQuery,city.ilike.$searchQuery,province.ilike.$searchQuery)")
            }

            if (filters.flags.isNotEmpty()) {
                val flagConditions = filters.flags.joinToString(",") { "flag.eq.$it" }
                conditions.add("($flagConditions)")
            }

            if (filters.serviceTypes.isNotEmpty()) {
                val serviceConditions =
                    filters.serviceTypes.joinToString(",") { "service_type.eq.$it" }
                conditions.add("($serviceConditions)")
            }

            var stations = if (conditions.isNotEmpty()) {
                client.from("gas_stations").select {
                    filter {
                        or {
                            conditions.forEach { condition ->
                                raw(condition)
                            }
                        }
                    }
                }.decodeList<GasStation>()
            } else {
                client.from("gas_stations").select().decodeList<GasStation>()
            }

            if (filters.fuelTypes.isNotEmpty()) {
                stations = stations.filter { station ->
                    val stationFuels = getFuelPricesForStation(station.id.toLong())
                    filters.fuelTypes.any { fuelType ->
                        stationFuels.any { fuel -> fuel.type.equals(fuelType) }
                    }
                }
            }

            stations
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun clearCache() {
        stationCache.evictAll()
    }
}