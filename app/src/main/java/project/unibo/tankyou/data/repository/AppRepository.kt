package project.unibo.tankyou.data.repository

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import android.util.Log
import io.github.jan.supabase.postgrest.query.Order
import project.unibo.tankyou.data.DatabaseClient
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.database.entities.Fuel

class AppRepository {
    private val client = DatabaseClient.client

    // Funzione originale (mantieni per compatibilità)
    // suspend fun getAllStations(): List<GasStation> {
    //     return client.from("gas_stations").select().decodeList<GasStation>()
    // }

    // Nuova funzione con paginazione usando limit e offset
    suspend fun getAllStations(): List<GasStation> {
        val allStations = mutableListOf<GasStation>()
        var lastId = 0L
        val batchSize = 1000

        do {
            val batch = client.from("gas_stations")
                .select {
                    filter {
                        gt("id", lastId)
                    }
                    limit(batchSize.toLong())
                    order("id", Order.ASCENDING)
                }
                .decodeList<GasStation>()

            if (batch.isNotEmpty()) {
                allStations.addAll(batch)
                lastId = batch.last().id
                Log.d("AppRepository", "Caricato batch di ${batch.size} stazioni (totale: ${allStations.size}, ultimo ID: $lastId)")
            }

        } while (batch.size == batchSize)

        return allStations
    }

    // Funzione per caricare stazioni in una specifica area geografica
    suspend fun getStationsInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<GasStation> {
        return client.from("gas_stations")
            .select {
                filter {
                    and {
                        gte("latitude", minLat)
                        lte("latitude", maxLat)
                        gte("longitude", minLon)
                        lte("longitude", maxLon)
                    }
                }
            }
            .decodeList<GasStation>()
    }

    // Funzione per caricare stazioni per province specifiche
    suspend fun getStationsByProvinces(provinces: List<String>): List<GasStation> {
        return client.from("gas_stations")
            .select {
                filter {
                    isIn("province", provinces)
                }
            }
            .decodeList<GasStation>()
    }

    // Resto delle funzioni esistenti...
    suspend fun getStationById(id: String): GasStation? {
        return client.from("gas_stations").select {
            filter { eq("id", id) }
        }.decodeSingleOrNull<GasStation>()
    }

    suspend fun getPricesByStationId(stationId: String): List<Fuel> {
        return client.from("fuels").select {
            filter { eq("stationId", stationId) }
        }.decodeList<Fuel>()
    }

    suspend fun getSpecificPrice(stationId: String, fuelType: Int, isSelf: Boolean): Fuel? {
        return client.from("fuels").select {
            filter {
                eq("stationId", stationId)
                eq("type", fuelType)
                eq("self", isSelf)
            }
        }.decodeSingleOrNull<Fuel>()
    }

    suspend fun insertStation(station: GasStation): GasStation {
        return client.from("gas_stations").insert(station) {
            select()
        }.decodeSingle<GasStation>()
    }

    suspend fun insertFuel(fuel: Fuel): Fuel {
        return client.from("fuels").insert(fuel) {
            select()
        }.decodeSingle<Fuel>()
    }

    suspend fun getStationCount(): Int {
        return try {
            client.from("gas_stations").select {
                count(Count.EXACT)
            }.countOrNull()?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getFuelCount(): Int {
        return try {
            client.from("fuels").select {
                count(Count.EXACT)
            }.countOrNull()?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun isDataEmpty(): Boolean {
        return getStationCount() == 0 || getFuelCount() == 0
    }

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(): AppRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository().also { INSTANCE = it }
            }
        }
    }
}