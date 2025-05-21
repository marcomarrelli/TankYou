package project.unibo.tankyou.data.database.utils

import project.unibo.tankyou.data.database.daos.*
import project.unibo.tankyou.data.database.entities.*
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader

class CsvDataManager(
    private val gasStationDAO: GasStationDAO,
    private val fuelDAO: FuelDAO
) {
    private val CSV_DELIMITER: Char = ';'

    suspend fun processStationsFile(file: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val stations = parseFuelStationsFromCsv(file)
            val updatedCount = updateStationsInDatabase(stations)
            Result.success(updatedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processPricesFile(file: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val prices = parseFuelPricesFromCsv(file)
            val updatedCount = updatePricesInDatabase(prices)
            Result.success(updatedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseFuelStationsFromCsv(file: File): List<GasStation> {
        val stations = mutableListOf<GasStation>()

        try {
            // Configura il parser per utilizzare ";" come separatore
            val csvParser = CSVParserBuilder()
                .withSeparator(CSV_DELIMITER)
                .build()

            val reader = CSVReaderBuilder(FileReader(file))
                .withSkipLines(1) // Salta l'intestazione
                .withCSVParser(csvParser) // Usa il parser configurato
                .build()

            var nextLine: Array<String>?
            while (reader.readNext().also { nextLine = it } != null) {
                nextLine?.let {
                    try {
                        val station = GasStation.fromCsvRow(it)
                        if (station != null) {
                            stations.add(station)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return stations
    }

    private fun parseFuelPricesFromCsv(file: File): List<Fuel> {
        val prices = mutableListOf<Fuel>()

        try {
            // Configura il parser per utilizzare ";" come separatore
            val csvParser = CSVParserBuilder()
                .withSeparator(CSV_DELIMITER)
                .build()

            val reader = CSVReaderBuilder(FileReader(file))
                .withSkipLines(1) // Salta l'intestazione
                .withCSVParser(csvParser) // Usa il parser configurato
                .build()

            var nextLine: Array<String>?
            while (reader.readNext().also { nextLine = it } != null) {
                nextLine?.let {
                    try {
                        val price = Fuel.fromCsvRow(it)
                        if (price != null) {
                            prices.add(price)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return prices
    }

    private suspend fun updateStationsInDatabase(newStations: List<GasStation>): Int {
        var count = 0

        val batchSize = 50
        val batches = newStations.chunked(batchSize)

        for (batch in batches) {
            val stationsToUpdate = mutableListOf<GasStation>()
            val stationsToInsert = mutableListOf<GasStation>()

            for (station in batch) {
                val existingStation = gasStationDAO.getStationById(station.id)

                if (existingStation != null) {
                    if (existingStation != station) {
                        stationsToUpdate.add(station)
                    }
                } else {
                    stationsToInsert.add(station)
                }
            }

            if (stationsToUpdate.isNotEmpty()) {
                gasStationDAO.updateStations(stationsToUpdate)
                count += stationsToUpdate.size
            }

            if (stationsToInsert.isNotEmpty()) {
                gasStationDAO.insertAll(stationsToInsert)
                count += stationsToInsert.size
            }
        }

        return count
    }

    private suspend fun updatePricesInDatabase(newPrices: List<Fuel>): Int {
        var count = 0

        val batchSize = 100
        val batches = newPrices.chunked(batchSize)

        for (batch in batches) {
            val pricesToUpdate = mutableListOf<Fuel>()
            val pricesToInsert = mutableListOf<Fuel>()

            for (price in batch) {
                val existingPrice = fuelDAO.getSpecificPrice(
                    price.stationID,
                    price.type,
                    price.self
                )

                if (existingPrice != null) {
                    if (existingPrice.price != price.price || existingPrice.date != price.date) {
                        val updatedPrice = price.copy(id = existingPrice.id)
                        pricesToUpdate.add(updatedPrice)
                    }
                } else {
                    pricesToInsert.add(price)
                }
            }

            if (pricesToUpdate.isNotEmpty()) {
                fuelDAO.updatePrices(pricesToUpdate)
                count += pricesToUpdate.size
            }

            if (pricesToInsert.isNotEmpty()) {
                fuelDAO.insertAll(pricesToInsert)
                count += pricesToInsert.size
            }
        }

        return count
    }
}