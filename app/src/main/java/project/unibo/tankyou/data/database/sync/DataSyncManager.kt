package project.unibo.tankyou.data.database.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import project.unibo.tankyou.data.database.AppDatabase
import project.unibo.tankyou.data.database.utils.CsvDataManager
import project.unibo.tankyou.data.database.utils.FileDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DataSyncManager(private val context: Context) {
    private val fileDownloader = FileDownloader()
    private val database = AppDatabase.getDatabase(context)
    private val csvDataManager = CsvDataManager(
        database.gasStationDAO(),
        database.fuelDAO()
    )

    suspend fun syncData(
        stationsUrl: String,
        pricesUrl: String,
        autoDownload: Boolean = true
    ): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            val stationsFileName = "stations.csv"
            val pricesFileName = "prices.csv"

            if (autoDownload) {
                val stationsFileResult = fileDownloader.downloadFile(context, stationsUrl, stationsFileName)
                if (stationsFileResult.isFailure) {
                    return@withContext Result.failure(stationsFileResult.exceptionOrNull()
                        ?: Exception("Failed to download stations file"))
                }

                val pricesFileResult = fileDownloader.downloadFile(context, pricesUrl, pricesFileName)
                if (pricesFileResult.isFailure) {
                    return@withContext Result.failure(pricesFileResult.exceptionOrNull()
                        ?: Exception("Failed to download prices file"))
                }
            }

            val stationsFile = context.filesDir.resolve(stationsFileName)
            val pricesFile = context.filesDir.resolve(pricesFileName)

            if (!stationsFile.exists() || !pricesFile.exists()) {
                return@withContext Result.failure(Exception("CSV files not found"))
            }

            val stationsResult = csvDataManager.processStationsFile(stationsFile)
            if (stationsResult.isFailure) {
                return@withContext Result.failure(stationsResult.exceptionOrNull()
                    ?: Exception("Failed to process stations"))
            }

            val pricesResult = csvDataManager.processPricesFile(pricesFile)
            if (pricesResult.isFailure) {
                return@withContext Result.failure(pricesResult.exceptionOrNull()
                    ?: Exception("Failed to process prices"))
            }

            val syncResult = SyncResult(
                stationsUpdated = stationsResult.getOrDefault(0),
                pricesUpdated = pricesResult.getOrDefault(0),
                timestamp = System.currentTimeMillis()
            )

            Result.success(syncResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun schedulePeriodicSync(stationsUrl: String, pricesUrl: String, intervalHours: Int = 24) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInputData(DataSyncWorker.createInputData(stationsUrl, pricesUrl))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DATA_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncWorkRequest
        )
    }

    companion object {
        const val DATA_SYNC_WORK_NAME = "data_sync_work"
    }

    data class SyncResult(
        val stationsUpdated: Int,
        val pricesUpdated: Int,
        val timestamp: Long
    )
}