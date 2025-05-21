package project.unibo.tankyou.data.database.daos

import androidx.room.*
import project.unibo.tankyou.data.database.entities.Fuel

@Dao
interface FuelDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(fuelPrices: List<Fuel>): List<Long>

    @Update
    suspend fun updatePrices(prices: List<Fuel>)

    @Query("SELECT * FROM fuels WHERE stationID = :stationId")
    suspend fun getPricesByStationId(stationId: String): List<Fuel>

    @Query("SELECT * FROM fuels WHERE stationID = :stationId AND type = :fuelType AND self = :isSelf")
    suspend fun getSpecificPrice(stationId: String, fuelType: String, isSelf: Boolean): Fuel?

    @Query("SELECT COUNT(*) FROM fuels")
    suspend fun getCount(): Int

    @Query("DELETE FROM fuels")
    suspend fun deleteAll()
}