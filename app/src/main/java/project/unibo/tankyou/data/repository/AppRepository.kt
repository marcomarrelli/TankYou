package project.unibo.tankyou.data.repository

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import android.util.Log
import android.util.LruCache
import io.github.jan.supabase.postgrest.query.Order
import org.osmdroid.util.BoundingBox
import project.unibo.tankyou.data.DatabaseClient
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.database.entities.Fuel

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

                Log.d("AppRepository", "Caricato batch di ${batch.size} stazioni (totale: ${allStations.size})")

            } while (batch.size == batchSize)

        } catch (e: Exception) {
            Log.e("AppRepository", "Errore nel caricamento delle stazioni", e)
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
            Log.e("AppRepository", "Errore nel caricamento stazioni in bounds", e)
            emptyList()
        }
    }

    suspend fun getStationsForZoomLevel(
        bounds: BoundingBox,
        zoomLevel: Double
    ): List<GasStation> {
        val roundedZoom = kotlin.math.round(zoomLevel * 2) / 2.0
        val cacheKey = "${bounds.latSouth}_${bounds.latNorth}_${bounds.lonWest}_${bounds.lonEast}_$roundedZoom"

        stationCache.get(cacheKey)?.let {
            Log.d("AppRepository", "Cache hit per zoom $roundedZoom")
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
            Log.d("AppRepository", "Caricate ${stations.size} stazioni per zoom $roundedZoom (limit: $limit)")
            stations

        } catch (e: Exception) {
            Log.e("AppRepository", "Errore nel caricamento stazioni per zoom", e)
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
        return client.from("gas_stations").select() {}.countOrNull() ?: 0L
    }

    suspend fun getAllFuels(): List<Fuel> {
        return client.from("fuel").select().decodeList<Fuel>()
    }

    suspend fun getFuelById(id: String): Fuel? {
        return client.from("fuel").select {
            filter { eq("id", id) }
        }.decodeSingleOrNull<Fuel>()
    }

    fun clearCache() {
        stationCache.evictAll()
        Log.d("AppRepository", "Cache svuotata")
    }
}