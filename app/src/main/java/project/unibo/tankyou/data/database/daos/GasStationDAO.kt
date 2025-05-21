package project.unibo.tankyou.data.database.daos

import androidx.room.*
import project.unibo.tankyou.data.database.entities.GasStation

@Dao
interface GasStationDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(stations: List<GasStation>): List<Long>

    @Update
    suspend fun updateStations(stations: List<GasStation>)

    @Query("SELECT * FROM gas_stations WHERE id = :id")
    suspend fun getStationById(id: String): GasStation?

    @Query("SELECT * FROM gas_stations")
    suspend fun getAllStations(): List<GasStation>

    @Query("SELECT COUNT(*) FROM gas_stations")
    suspend fun getCount(): Int

    @Query("DELETE FROM gas_stations")
    suspend fun deleteAll()
}