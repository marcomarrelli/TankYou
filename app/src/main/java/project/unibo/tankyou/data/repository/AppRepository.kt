package project.unibo.tankyou.data.repository

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import project.unibo.tankyou.data.DatabaseClient
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.database.entities.Fuel

class AppRepository {
    private val client = DatabaseClient.client

    suspend fun getAllStations(): List<GasStation> {
        return client.from("gas_stations").select().decodeList<GasStation>()
    }

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