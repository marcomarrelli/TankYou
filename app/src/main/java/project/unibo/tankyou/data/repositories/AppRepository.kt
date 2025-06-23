package project.unibo.tankyou.data.repositories

import android.util.Log
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
import project.unibo.tankyou.data.database.entities.UserSavedGasStation
import project.unibo.tankyou.utils.Constants

/**
 * Data class representing search filters for gas stations.
 *
 * @param flags List of gas station flags to filter by
 * @param fuelTypes List of fuel types to filter by
 * @param serviceTypes List of service type booleans to filter by
 * @param savedOnly Flag indicating whether to show only saved stations
 */
data class SearchFilters(
    val flags: List<GasStationFlag> = emptyList(),
    val fuelTypes: List<FuelType> = emptyList(),
    val serviceTypes: List<Boolean> = emptyList(),
    val savedOnly: Boolean = false
)

/**
 * Repository class responsible for managing gas station data operations.
 * Implements caching mechanisms and provides methods for fetching, searching, and filtering gas stations.
 */
class AppRepository {
    private val client = DatabaseClient.client
    private val stationCache: LruCache<String, List<GasStation>> = LruCache(50)
    private val searchCache: LruCache<String, List<GasStation>> = LruCache(20)
    private val fuelCache: LruCache<Long, List<Fuel>> = LruCache(100)

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        /**
         * Returns the singleton instance of AppRepository.
         * Thread-safe implementation using double-checked locking pattern.
         *
         * @return The singleton instance of AppRepository
         */
        fun getInstance(): AppRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository().also { INSTANCE = it }
            }
        }
    }

    /**
     * Retrieves gas stations within specified geographic bounds and zoom level.
     * Implements intelligent caching and limit adjustment based on zoom level.
     *
     * @param bounds The geographic bounding box to search within
     * @param zoomLevel The current map zoom level for determining result limit
     *
     * @return List of gas stations within the specified bounds
     */
    suspend fun getStationsForZoomLevel(
        bounds: BoundingBox,
        zoomLevel: Double
    ): List<GasStation> {
        val roundedZoom: Double = kotlin.math.round(zoomLevel * 2) / 2.0
        val cacheKey =
            "${bounds.latSouth}_${bounds.latNorth}_${bounds.lonWest}_${bounds.lonEast}_$roundedZoom"

        stationCache.get(cacheKey)?.let { cachedStations ->
            Log.d(
                Constants.App.LOG_TAG,
                "Retrieved ${cachedStations.size} stations from cache for zoom level $roundedZoom"
            )
            return cachedStations
        }

        val limit: Int = when {
            zoomLevel < 8 -> 10000
            zoomLevel < 10 -> 5000
            zoomLevel < 13 -> 1000
            zoomLevel < 16 -> 250
            else -> 100
        }

        return try {
            Log.d(
                Constants.App.LOG_TAG,
                "Fetching stations for bounds: $bounds, zoom: $zoomLevel, limit: $limit"
            )

            val stations: List<GasStation> = client.from("gas_stations")
                .select {
                    filter {
                        and {
                            gte("latitude", bounds.latSouth) // x >= s
                            lte("latitude", bounds.latNorth) // x <= n
                            gte("longitude", bounds.lonWest) // x >= w
                            lte("longitude", bounds.lonEast) // x <= e
                        }
                    }
                    limit(limit.toLong())
                    order("id", Order.ASCENDING)
                }
                .decodeList<GasStation>()

            stationCache.put(cacheKey, stations)
            Log.d(
                Constants.App.LOG_TAG,
                "Successfully fetched and cached ${stations.size} stations"
            )
            stations
        } catch (e: Exception) {
            Log.e(
                Constants.App.LOG_TAG,
                "Error fetching stations for bounds: $bounds, zoom: $zoomLevel",
                e
            )
            emptyList()
        }
    }

    /**
     * Retrieves a specific gas station by its unique identifier.
     *
     * @param id The unique identifier of the gas station
     *
     * @return The gas station if found, null otherwise
     */
    suspend fun getStationById(id: String): GasStation? {
        return try {
            Log.d(Constants.App.LOG_TAG, "Fetching station with ID: $id")

            val station: GasStation? = client.from("gas_stations").select {
                filter { eq("id", id) }
            }.decodeSingleOrNull<GasStation>()

            if (station != null) {
                Log.d(Constants.App.LOG_TAG, "Successfully retrieved station: ${station.name}")
            } else {
                Log.w(Constants.App.LOG_TAG, "No station found with ID: $id")
            }

            station
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error fetching station with ID: $id", e)
            null
        }
    }

    /**
     * Retrieves fuel prices for a specific gas station.
     * Implements caching to improve performance for repeated requests.
     *
     * @param stationId The unique identifier of the gas station
     *
     * @return List of fuel prices for the specified station
     */
    suspend fun getFuelPricesForStation(stationId: Long): List<Fuel> {
        fuelCache.get(stationId)?.let { cachedFuels ->
            Log.d(
                Constants.App.LOG_TAG,
                "Retrieved ${cachedFuels.size} fuel prices from cache for station $stationId"
            )
            return cachedFuels
        }

        return try {
            Log.d(Constants.App.LOG_TAG, "Fetching fuel prices for station: $stationId")

            val fuels: List<Fuel> = client.from("fuels")
                .select {
                    filter { eq("station_id", stationId) }
                    order("type", Order.ASCENDING)
                }
                .decodeList<Fuel>()

            fuelCache.put(stationId, fuels)
            Log.d(
                Constants.App.LOG_TAG,
                "Successfully fetched and cached ${fuels.size} fuel prices for station $stationId"
            )
            fuels
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error fetching fuel prices for station: $stationId", e)
            emptyList()
        }
    }

    /**
     * Retrieves all available fuel types from the database.
     *
     * @return List of all fuel types
     */
    suspend fun getFuelTypes(): List<FuelType> {
        return try {
            Log.d(Constants.App.LOG_TAG, "Fetching fuel types")

            val fuelTypes: List<FuelType> = client.from("fuel_types")
                .select()
                .decodeList<FuelType>()

            Log.d(Constants.App.LOG_TAG, "Successfully fetched ${fuelTypes.size} fuel types")
            fuelTypes
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error fetching fuel types", e)
            emptyList()
        }
    }

    /**
     * Retrieves all available gas station types from the database.
     *
     * @return List of all gas station types
     */
    suspend fun getGasStationTypes(): List<GasStationType> {
        return try {
            Log.d(Constants.App.LOG_TAG, "Fetching gas station types")

            val stationTypes: List<GasStationType> = client.from("gas_station_types")
                .select()
                .decodeList<GasStationType>()

            Log.d(
                Constants.App.LOG_TAG,
                "Successfully fetched ${stationTypes.size} gas station types"
            )
            stationTypes
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error fetching gas station types", e)
            emptyList()
        }
    }

    /**
     * Retrieves all available gas station flags from the database.
     *
     * @return List of all gas station flags
     */
    suspend fun getFlags(): List<GasStationFlag> {
        return try {
            Log.d(Constants.App.LOG_TAG, "Fetching gas station flags")

            val flags: List<GasStationFlag> = client.from("gas_station_flags")
                .select()
                .decodeList<GasStationFlag>()

            Log.d(Constants.App.LOG_TAG, "Successfully fetched ${flags.size} gas station flags")
            flags
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error fetching gas station flags", e)
            emptyList()
        }
    }

    /**
     * Searches for gas stations by name, city, or province.
     * Implements caching to improve performance for repeated searches.
     *
     * @param query The search query string
     *
     * @return List of gas stations matching the search criteria
     */
    suspend fun searchStations(query: String): List<GasStation> {
        if (query.isBlank()) {
            Log.w(Constants.App.LOG_TAG, "Search query is blank, returning empty list")
            return emptyList()
        }

        val cacheKey = "search_$query"
        searchCache.get(cacheKey)?.let { cachedResults ->
            Log.d(
                Constants.App.LOG_TAG,
                "Retrieved ${cachedResults.size} search results from cache for query: $query"
            )
            return cachedResults
        }

        val searchQuery = "%${query.lowercase()}%"

        return try {
            Log.d(Constants.App.LOG_TAG, "Searching stations with query: $query")

            val results: List<GasStation> = client.from("gas_stations")
                .select {
                    filter {
                        or {
                            ilike("name", searchQuery)
                            ilike("city", searchQuery)
                            ilike("province", searchQuery)
                        }
                    }
                    // limit(x), maybe a const val for limits? x = 1000
                }
                .decodeList<GasStation>()

            searchCache.put(cacheKey, results)
            Log.d(
                Constants.App.LOG_TAG,
                "Successfully found and cached ${results.size} stations for query: $query"
            )
            results
        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error searching stations with query: $query", e)
            emptyList()
        }
    }

    /**
     * Searches within saved stations using specified query and filters.
     * Only searches among stations that the user has previously saved.
     *
     * @param query The search query string
     * @param filters Additional filters to apply to the search
     *
     * @return List of saved gas stations matching the search criteria and filters
     */
    suspend fun searchSavedStationsWithFilters(
        query: String,
        filters: SearchFilters
    ): List<GasStation> = coroutineScope {
        try {
            Log.d(
                Constants.App.LOG_TAG,
                "Searching saved stations with query: '$query' and filters: $filters"
            )

            val userRepository: UserRepository = UserRepository.getInstance()
            val savedStations: List<UserSavedGasStation> = userRepository.getUserSavedStations()

            if (savedStations.isEmpty()) {
                Log.w(Constants.App.LOG_TAG, "No saved stations found for user")
                return@coroutineScope emptyList()
            }

            Log.d(Constants.App.LOG_TAG, "Found ${savedStations.size} saved stations")
            val savedStationIds: List<Long> = savedStations.map { it.stationId }

            val allSavedStations: List<GasStation> = savedStationIds.chunked(100).flatMap { chunk ->
                client.from("gas_stations")
                    .select {
                        filter {
                            or {
                                chunk.forEach { stationId ->
                                    eq("id", stationId)
                                }
                            }
                        }
                        // limit(x), maybe a const val for limits? x = 1000
                    }
                    .decodeList<GasStation>()
            }

            var filteredStations: List<GasStation> = if (query.isNotBlank()) {
                val searchQuery: String = query.lowercase()
                allSavedStations.filter { station ->
                    (station.name?.lowercase()?.contains(searchQuery) == true) ||
                            (station.city?.lowercase()?.contains(searchQuery) == true) ||
                            (station.province?.lowercase()?.contains(searchQuery) == true)
                }
            } else {
                allSavedStations
            }

            if (filters.flags.isNotEmpty()) {
                val flagIds: List<Int> = filters.flags.map { it.id }
                filteredStations = filteredStations.filter { station ->
                    station.flag in flagIds
                }
                Log.d(
                    Constants.App.LOG_TAG,
                    "Applied flag filters, ${filteredStations.size} stations remaining"
                )
            }

            if (filters.fuelTypes.isNotEmpty()) {
                val fuelTypeIds: List<Int> = filters.fuelTypes.map { it.id }

                val stationFuelsDeferred = filteredStations.map { station ->
                    async {
                        val cachedFuels: List<Fuel>? = fuelCache.get(station.id.toLong())
                        if (cachedFuels != null) {
                            station to cachedFuels
                        } else {
                            val fuels: List<Fuel> = try {
                                client.from("fuels")
                                    .select {
                                        filter { eq("station_id", station.id.toLong()) }
                                    }
                                    .decodeList<Fuel>()
                            } catch (e: Exception) {
                                Log.e(
                                    Constants.App.LOG_TAG,
                                    "Error fetching fuels for station ${station.id}",
                                    e
                                )
                                emptyList()
                            }
                            fuelCache.put(station.id.toLong(), fuels)
                            station to fuels
                        }
                    }
                }

                val stationsWithFuels: List<Pair<GasStation, List<Fuel>>> =
                    stationFuelsDeferred.map { it.await() }

                filteredStations = stationsWithFuels.filter { (_, fuels) ->
                    fuelTypeIds.any { fuelTypeId ->
                        fuels.any { fuel -> fuel.type == fuelTypeId }
                    }
                }.map { it.first }

                Log.d(
                    Constants.App.LOG_TAG,
                    "Applied fuel type filters, ${filteredStations.size} stations remaining"
                )
            }

            Log.d(
                Constants.App.LOG_TAG,
                "Search completed successfully, returning ${filteredStations.size} saved stations"
            )
            filteredStations

        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error searching saved stations with filters", e)
            emptyList()
        }
    }

    /**
     * Searches for gas stations using specified query and filters.
     * Applies multiple filter criteria including flags, fuel types, and service types.
     *
     * @param query The search query string
     * @param filters Additional filters to apply to the search
     *
     * @return List of gas stations matching the search criteria and filters
     */
    suspend fun searchStationsWithFilters(
        query: String,
        filters: SearchFilters
    ): List<GasStation> = coroutineScope {
        if (query.isBlank() && filters.flags.isEmpty() && filters.fuelTypes.isEmpty() && filters.serviceTypes.isEmpty()) {
            Log.w(Constants.App.LOG_TAG, "No search criteria provided, returning empty list")
            return@coroutineScope emptyList()
        }

        val cacheKey =
            "filtered_${query}_${filters.flags.joinToString { it.id.toString() }}_${filters.fuelTypes.joinToString { it.id.toString() }}"
        searchCache.get(cacheKey)?.let { cachedResults ->
            Log.d(
                Constants.App.LOG_TAG,
                "Retrieved ${cachedResults.size} filtered search results from cache"
            )
            return@coroutineScope cachedResults
        }

        try {
            Log.d(
                Constants.App.LOG_TAG,
                "Searching stations with filters - Query: '$query', Filters: $filters"
            )

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
                            // limit(x), maybe a const val for limits? x = 1000
                        }
                        .decodeList<GasStation>()
                } else {
                    client.from("gas_stations").select().decodeList<GasStation>()
                    // limit(x), maybe a const val for limits? x = 1000
                }
            }

            var filteredStations: List<GasStation> = baseStationsDeferred.await()
            Log.d(Constants.App.LOG_TAG, "Base search returned ${filteredStations.size} stations")

            if (filters.flags.isNotEmpty()) {
                val flagIds: List<Int> = filters.flags.map { it.id }
                filteredStations = filteredStations.filter { station ->
                    station.flag in flagIds
                }
                Log.d(
                    Constants.App.LOG_TAG,
                    "Applied flag filters, ${filteredStations.size} stations remaining"
                )
            }

            if (filters.fuelTypes.isNotEmpty()) {
                val fuelTypeIds: List<Int> = filters.fuelTypes.map { it.id }

                val stationFuelsDeferred = filteredStations.map { station ->
                    async {
                        val cachedFuels: List<Fuel>? = fuelCache.get(station.id.toLong())
                        if (cachedFuels != null) {
                            station to cachedFuels
                        } else {
                            val fuels: List<Fuel> = try {
                                client.from("fuels")
                                    .select {
                                        filter { eq("station_id", station.id.toLong()) }
                                    }
                                    .decodeList<Fuel>()
                            } catch (e: Exception) {
                                Log.e(
                                    Constants.App.LOG_TAG,
                                    "Error fetching fuels for station ${station.id}",
                                    e
                                )
                                emptyList()
                            }
                            fuelCache.put(station.id.toLong(), fuels)
                            station to fuels
                        }
                    }
                }

                val stationsWithFuels: List<Pair<GasStation, List<Fuel>>> =
                    stationFuelsDeferred.map { it.await() }

                filteredStations = stationsWithFuels.filter { (_, fuels) ->
                    fuelTypeIds.any { fuelTypeId ->
                        fuels.any { fuel -> fuel.type == fuelTypeId }
                    }
                }.map { it.first }

                Log.d(
                    Constants.App.LOG_TAG,
                    "Applied fuel type filters, ${filteredStations.size} stations remaining"
                )
            }

            searchCache.put(cacheKey, filteredStations)
            Log.d(
                Constants.App.LOG_TAG,
                "Search with filters completed successfully, returning ${filteredStations.size} stations"
            )
            filteredStations

        } catch (e: Exception) {
            Log.e(Constants.App.LOG_TAG, "Error searching stations with filters", e)
            emptyList()
        }
    }
}