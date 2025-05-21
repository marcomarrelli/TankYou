package project.unibo.tankyou.data.repository


import android.content.Context
import project.unibo.tankyou.data.database.AppDatabase
import project.unibo.tankyou.data.database.daos.FuelDAO
import project.unibo.tankyou.data.database.daos.GasStationDAO
import project.unibo.tankyou.data.database.entities.Fuel
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.database.sync.DataSyncManager

class AppRepository(
    private val context: Context,
    private val gasStationDAO: GasStationDAO,
    private val fuelDAO: FuelDAO
) {
    private val dataSyncManager = DataSyncManager(context)

    suspend fun getAllStations(): List<GasStation> {
        return gasStationDAO.getAllStations()
    }

    suspend fun getStationById(id: String): GasStation? {
        return gasStationDAO.getStationById(id)
    }

    suspend fun getPricesByStationId(stationId: String): List<Fuel> {
        return fuelDAO.getPricesByStationId(stationId)
    }

    suspend fun syncData(stationsUrl: String, pricesUrl: String, autoDownload: Boolean = true): Result<DataSyncManager.SyncResult> {
        return dataSyncManager.syncData(stationsUrl, pricesUrl, autoDownload)
    }

    fun schedulePeriodicSync(stationsUrl: String, pricesUrl: String, intervalHours: Int = 24) {
        dataSyncManager.schedulePeriodicSync(stationsUrl, pricesUrl, intervalHours)
    }

    suspend fun isDataEmpty(): Boolean {
        return gasStationDAO.getCount() == 0 || fuelDAO.getCount() == 0
    }

    suspend fun clearDatabase() {
        gasStationDAO.deleteAll()
        fuelDAO.deleteAll()
    }

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.getDatabase(context)
                val instance = AppRepository(
                    context.applicationContext,
                    database.gasStationDAO(),
                    database.fuelDAO()
                )
                INSTANCE = instance
                instance
            }
        }
    }
}