package com.gorunjinian.dbst.data

import android.content.Context
import android.util.Log
import com.opencsv.CSVReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

object DatabaseInitializer {
    private const val TAG = "DatabaseInitializer"
    private const val DBT_CSV_FILE = "dbt_data.csv"

    suspend fun initializeDbIfNeeded(context: Context, database: AppDatabase) {
        withContext(Dispatchers.IO) {
            val appDao = database.appDao()

            // Check if DBT table is empty
            val dbtCount = appDao.getAllIncome().size
            if (dbtCount == 0) {
                Log.d(TAG, "DBT table is empty, initializing with CSV data")
                loadDbtDataFromCsv(context, appDao)
            } else {
                Log.d(TAG, "DBT table already has $dbtCount entries, skipping initialization")
            }
        }
    }

    private suspend fun loadDbtDataFromCsv(context: Context, appDao: AppDao) {
        try {
            val inputStream = context.assets.open(DBT_CSV_FILE)
            val reader = CSVReader(InputStreamReader(inputStream))

            // Skip header row if present
            var nextLine = reader.readNext()

            // Check if the first row is a header
            if (nextLine != null && nextLine[0].equals("id", ignoreCase = true)) {
                nextLine = reader.readNext()
            }

            var count = 0
            while (nextLine != null) {
                // Parse CSV data into DBT object
                try {
                    val id = nextLine[0].toIntOrNull() ?: 0  // Parse ID if available
                    val date = nextLine[1]
                    val person = nextLine[2]
                    val amount = nextLine[3].toDouble()
                    val rate = nextLine[4].toDoubleOrNull() ?: 1.0
                    val type = nextLine[5]

                    val dbtEntry = DBT(
                        id = id,  // Use ID from CSV
                        date = date,
                        person = person,
                        amount = amount,
                        rate = rate,
                        type = type,
                        // totalLBP will be calculated by the entity
                    )

                    appDao.insertIncome(dbtEntry)
                    count++
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing CSV line: ${e.message}")
                }

                nextLine = reader.readNext()
            }

            try {
                val maxId = appDao.getMaxDbtId() // You'll need to add this function to AppDao
                if (maxId > 0) {
                    // Reset sequence to continue from the max ID
                    appDao.resetDbtSequenceTo(maxId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update sequence: ${e.message}")
            }

            reader.close()
            Log.d(TAG, "Successfully imported $count DBT entries from CSV")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading CSV data: ${e.message}")
        }
    }
}