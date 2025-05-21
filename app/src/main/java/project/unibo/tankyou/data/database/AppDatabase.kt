package project.unibo.tankyou.data.database

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

import project.unibo.tankyou.data.database.daos.FuelDAO
import project.unibo.tankyou.data.database.daos.GasStationDAO

import project.unibo.tankyou.data.database.entities.Fuel
import project.unibo.tankyou.data.database.entities.GasStation

import project.unibo.tankyou.data.database.utils.DateTimeConverter

@Database(
    entities = [Fuel::class, GasStation::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fuelDAO(): FuelDAO
    abstract fun gasStationDAO(): GasStationDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tank_you_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}