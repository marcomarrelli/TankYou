package project.unibo.tankyou.data.repositories

import android.util.LruCache
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    val serviceTypes: List<Boolean> = emptyList(),
    val savedOnly: Boolean = false
)

class AppRepository {
    private val client = DatabaseClient.client
    private val stationCache = LruCache<String, List<GasStation>>(50)
    private val searchCache = LruCache<String, List<GasStation>>(20)
    private val fuelCache = LruCache<Long, List<Fuel>>(100)

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
        fuelCache.get(stationId)?.let { return it }

        return try {
            val fuels = client.from("fuels")
                .select {
                    filter { eq("station_id", stationId) }
                    order("type", Order.ASCENDING)
                }
                .decodeList<Fuel>()

            fuelCache.put(stationId, fuels)
            fuels
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

        val cacheKey = "search_$query"
        searchCache.get(cacheKey)?.let { return it }

        val searchQuery = "%${query.lowercase()}%"

        return try {
            val results = client.from("gas_stations")
                .select {
                    filter {
                        or {
                            ilike("name", searchQuery)
                            ilike("city", searchQuery)
                            ilike("province", searchQuery)
                        }
                    }
                    limit(500)
                }
                .decodeList<GasStation>()

            searchCache.put(cacheKey, results)
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun searchSavedStationsWithFilters(
        query: String,
        filters: SearchFilters
    ): List<GasStation> = coroutineScope {
        try {
            val userRepository = UserRepository.getInstance()
            val savedStations = userRepository.getUserSavedStations()

            if (savedStations.isEmpty()) {
                return@coroutineScope emptyList()
            }

            val savedStationIds = savedStations.map { it.stationId }

            val allSavedStations = savedStationIds.chunked(100).flatMap { chunk ->
                client.from("gas_stations")
                    .select {
                        filter {
                            or {
                                chunk.forEach { stationId ->
                                    eq("id", stationId)
                                }
                            }
                        }
                        limit(1000)
                    }
                    .decodeList<GasStation>()
            }

            var filteredStations = if (query.isNotBlank()) {
                val searchQuery = query.lowercase()
                allSavedStations.filter { station ->
                    (station.name?.lowercase()?.contains(searchQuery) == true) ||
                            (station.city?.lowercase()?.contains(searchQuery) == true) ||
                            (station.province?.lowercase()?.contains(searchQuery) == true)
                }
            } else {
                allSavedStations
            }

            if (filters.flags.isNotEmpty()) {
                val flagIds = filters.flags.map { it.id }
                filteredStations = filteredStations.filter { station ->
                    station.flag in flagIds
                }
            }

            if (filters.fuelTypes.isNotEmpty()) {
                val fuelTypeIds = filters.fuelTypes.map { it.id }

                val stationFuelsDeferred = filteredStations.map { station ->
                    async {
                        val cachedFuels = fuelCache.get(station.id.toLong())
                        if (cachedFuels != null) {
                            station to cachedFuels
                        } else {
                            val fuels = try {
                                client.from("fuels")
                                    .select {
                                        filter { eq("station_id", station.id.toLong()) }
                                    }
                                    .decodeList<Fuel>()
                            } catch (e: Exception) {
                                emptyList()
                            }
                            fuelCache.put(station.id.toLong(), fuels)
                            station to fuels
                        }
                    }
                }

                val stationsWithFuels = stationFuelsDeferred.map { it.await() }

                filteredStations = stationsWithFuels.filter { (_, fuels) ->
                    fuelTypeIds.any { fuelTypeId ->
                        fuels.any { fuel -> fuel.type == fuelTypeId }
                    }
                }.map { it.first }
            }

            filteredStations

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun searchStationsWithFilters(
        query: String,
        filters: SearchFilters
    ): List<GasStation> = coroutineScope {
        if (query.isBlank() && filters.flags.isEmpty() && filters.fuelTypes.isEmpty() && filters.serviceTypes.isEmpty()) {
            return@coroutineScope emptyList()
        }

        val cacheKey =
            "filtered_${query}_${filters.flags.joinToString { it.id.toString() }}_${filters.fuelTypes.joinToString { it.id.toString() }}"
        searchCache.get(cacheKey)?.let { return@coroutineScope it }

        try {
            val baseStationsDeferred = async {
                if (query.isNotBlank()) {
                    val searchQuery = "%${query.lowercase()}%"
                    client.from("gas_stations")
                        .select {
                            filter {
                                or {
                                    ilike("name", searchQuery)
                                    ilike("city", searchQuery)
                                    ilike("province", searchQuery)
                                }
                            }
                            limit(1000)
                        }
                        .decodeList<GasStation>()
                } else {
                    client.from("gas_stations")
                        .select {
                            limit(2000)
                        }
                        .decodeList<GasStation>()
                }
            }

            var filteredStations = baseStationsDeferred.await()

            if (filters.flags.isNotEmpty()) {
                val flagIds = filters.flags.map { it.id }
                filteredStations = filteredStations.filter { station ->
                    station.flag in flagIds
                }
            }

            if (filters.fuelTypes.isNotEmpty()) {
                val fuelTypeIds = filters.fuelTypes.map { it.id }

                val stationFuelsDeferred = filteredStations.map { station ->
                    async {
                        val cachedFuels = fuelCache.get(station.id.toLong())
                        if (cachedFuels != null) {
                            station to cachedFuels
                        } else {
                            val fuels = try {
                                client.from("fuels")
                                    .select {
                                        filter { eq("station_id", station.id.toLong()) }
                                    }
                                    .decodeList<Fuel>()
                            } catch (e: Exception) {
                                emptyList()
                            }
                            fuelCache.put(station.id.toLong(), fuels)
                            station to fuels
                        }
                    }
                }

                val stationsWithFuels = stationFuelsDeferred.map { it.await() }

                filteredStations = stationsWithFuels.filter { (_, fuels) ->
                    fuelTypeIds.any { fuelTypeId ->
                        fuels.any { fuel -> fuel.type == fuelTypeId }
                    }
                }.map { it.first }
            }

            searchCache.put(cacheKey, filteredStations)
            filteredStations

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun clearCache() {
        stationCache.evictAll()
        searchCache.evictAll()
        fuelCache.evictAll()
    }
}