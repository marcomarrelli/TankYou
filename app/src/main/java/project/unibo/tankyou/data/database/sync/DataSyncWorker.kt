package project.unibo.tankyou.data.database.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val stationsUrl = inputData.getString(KEY_STATIONS_URL)?: return@withContext Result.failure()

            val pricesUrl = inputData.getString(KEY_PRICES_URL)?: return@withContext Result.failure()

            val syncManager = DataSyncManager(applicationContext)
            val syncResult = syncManager.syncData(stationsUrl, pricesUrl)

            if (syncResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val KEY_STATIONS_URL = "gas_stations_url"
        private const val KEY_PRICES_URL = "fuel_prices_url"

        fun createInputData(stationsUrl: String, pricesUrl: String): Data {
            return Data.Builder().putString(KEY_STATIONS_URL, stationsUrl).putString(KEY_PRICES_URL, pricesUrl).build()
        }
    }
}