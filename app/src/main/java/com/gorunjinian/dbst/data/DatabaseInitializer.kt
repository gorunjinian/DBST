package com.gorunjinian.dbst.data

import android.content.Context
import android.util.Log
import com.opencsv.CSVReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

object DatabaseInitializer {
    private const val TAG = "DatabaseInitializer"

    // CSV file names in assets folder
    private const val DBT_CSV_FILE = "dbt_data.csv"
    private const val DST_CSV_FILE = "dst_data.csv"
    private const val VBSTIN_CSV_FILE = "vbstin_data.csv"
    private const val VBSTOUT_CSV_FILE = "vbstout_data.csv"
    private const val USDT_CSV_FILE = "usdt_data.csv"

    suspend fun initializeDbIfNeeded(context: Context, database: AppDatabase) {
        withContext(Dispatchers.IO) {
            val appDao = database.appDao()

            // Initialize DBT (Income) table
            val dbtCount = appDao.getAllIncome().size
            if (dbtCount == 0) {
                Log.d(TAG, "DBT table is empty, initializing with CSV data")
                loadDbtDataFromCsv(context, appDao)
            } else {
                Log.d(TAG, "DBT table already has $dbtCount entries, skipping initialization")
            }

            // Initialize DST (Expense) table
            val dstCount = appDao.getAllExpense().size
            if (dstCount == 0) {
                Log.d(TAG, "DST table is empty, initializing with CSV data")
                loadDstDataFromCsv(context, appDao)
            } else {
                Log.d(TAG, "DST table already has $dstCount entries, skipping initialization")
            }

            // Initialize VBSTIN (Credit In) table
            val vbstinCount = appDao.getAllVbstIn().size
            if (vbstinCount == 0) {
                Log.d(TAG, "VBSTIN table is empty, initializing with CSV data")
                loadVbstInDataFromCsv(context, appDao)
            } else {
                Log.d(TAG, "VBSTIN table already has $vbstinCount entries, skipping initialization")
            }

            // Initialize VBSTOUT (Credit Out) table
            val vbstoutCount = appDao.getAllVbstOut().size
            if (vbstoutCount == 0) {
                Log.d(TAG, "VBSTOUT table is empty, initializing with CSV data")
                loadVbstOutDataFromCsv(context, appDao)
            } else {
                Log.d(TAG, "VBSTOUT table already has $vbstoutCount entries, skipping initialization")
            }

            // Initialize USDT (Tether) table
            val usdtCount = appDao.getAllUsdt().size
            if (usdtCount == 0) {
                Log.d(TAG, "USDT table is empty, initializing with CSV data")
                loadUsdtDataFromCsv(context, appDao)
            } else {
                Log.d(TAG, "USDT table already has $usdtCount entries, skipping initialization")
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
                    val id = nextLine[0].toIntOrNull() ?: 0
                    val date = nextLine[1]
                    val person = nextLine[2]
                    val amount = nextLine[3].toDouble()
                    val rate = nextLine[4].toDoubleOrNull() ?: 1.0
                    val type = nextLine[5]

                    val dbtEntry = DBT(
                        id = id,
                        date = date,
                        person = person,
                        amount = amount,
                        rate = rate,
                        type = type
                    )

                    appDao.insertIncome(dbtEntry)
                    count++
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing DBT CSV line: ${e.message}")
                }

                nextLine = reader.readNext()
            }

            try {
                val maxId = appDao.getMaxDbtId()
                if (maxId > 0) {
                    appDao.resetDbtSequenceTo(maxId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update DBT sequence: ${e.message}")
            }

            reader.close()
            Log.d(TAG, "Successfully imported $count DBT entries from CSV")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading DBT CSV data: ${e.message}")
        }
    }

    private suspend fun loadDstDataFromCsv(context: Context, appDao: AppDao) {
        try {
            val inputStream = context.assets.open(DST_CSV_FILE)
            val reader = CSVReader(InputStreamReader(inputStream))

            // Skip header row if present
            var nextLine = reader.readNext()

            // Check if the first row is a header
            if (nextLine != null && nextLine[0].equals("id", ignoreCase = true)) {
                nextLine = reader.readNext()
            }

            var count = 0
            while (nextLine != null) {
                try {
                    val id = nextLine[0].toIntOrNull() ?: 0
                    val date = nextLine[1]
                    val person = nextLine[2]
                    val amountExpensed = nextLine[3].toDouble()
                    val amountExchanged = nextLine[4].toDouble()
                    val rate = nextLine[5].toDoubleOrNull() ?: 1.0
                    val type = nextLine[6]
                    // exchangedLBP will be calculated by the entity

                    val dstEntry = DST(
                        id = id,
                        date = date,
                        person = person,
                        amountExpensed = amountExpensed,
                        amountExchanged = amountExchanged,
                        rate = rate,
                        type = type
                    )

                    appDao.insertExpense(dstEntry)
                    count++
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing DST CSV line: ${e.message}")
                }

                nextLine = reader.readNext()
            }

            try {
                val maxId = appDao.getMaxDstId()
                if (maxId > 0) {
                    appDao.resetDstSequenceTo(maxId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update DST sequence: ${e.message}")
            }

            reader.close()
            Log.d(TAG, "Successfully imported $count DST entries from CSV")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading DST CSV data: ${e.message}")
        }
    }

    private suspend fun loadVbstInDataFromCsv(context: Context, appDao: AppDao) {
        try {
            val inputStream = context.assets.open(VBSTIN_CSV_FILE)
            val reader = CSVReader(InputStreamReader(inputStream))

            // Skip header row if present
            var nextLine = reader.readNext()

            // Check if the first row is a header
            if (nextLine != null && nextLine[0].equals("id", ignoreCase = true)) {
                nextLine = reader.readNext()
            }

            var count = 0
            while (nextLine != null) {
                try {
                    val id = nextLine[0].toIntOrNull() ?: 0
                    val date = nextLine[1]
                    val person = nextLine[2]
                    val type = nextLine[3]
                    val validity = nextLine[4]
                    val amount = nextLine[5].toDouble()
                    val total = nextLine[6].toDouble()
                    // rate will be calculated by the entity

                    val vbstinEntry = VBSTIN(
                        id = id,
                        date = date,
                        person = person,
                        type = type,
                        validity = validity,
                        amount = amount,
                        total = total
                    )

                    appDao.insertVbstIn(vbstinEntry)
                    count++
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing VBSTIN CSV line: ${e.message}")
                }

                nextLine = reader.readNext()
            }

            try {
                val maxId = appDao.getMaxVbstInId()
                if (maxId > 0) {
                    appDao.resetVbstInSequenceTo(maxId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update VBSTIN sequence: ${e.message}")
            }

            reader.close()
            Log.d(TAG, "Successfully imported $count VBSTIN entries from CSV")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading VBSTIN CSV data: ${e.message}")
        }
    }

    private suspend fun loadVbstOutDataFromCsv(context: Context, appDao: AppDao) {
        try {
            val inputStream = context.assets.open(VBSTOUT_CSV_FILE)
            val reader = CSVReader(InputStreamReader(inputStream))

            // Skip header row if present
            var nextLine = reader.readNext()

            // Check if the first row is a header
            if (nextLine != null && nextLine[0].equals("id", ignoreCase = true)) {
                nextLine = reader.readNext()
            }

            var count = 0
            while (nextLine != null) {
                try {
                    val id = nextLine[0].toIntOrNull() ?: 0
                    val date = nextLine[1]
                    val person = nextLine[2]
                    val amount = nextLine[3].toDouble()
                    val sellrate = nextLine[4].toDouble()
                    val type = nextLine[5]
                    val profit = nextLine[6].toDoubleOrNull() ?: 0.0

                    val vbstoutEntry = VBSTOUT(
                        id = id,
                        date = date,
                        person = person,
                        amount = amount,
                        sellrate = sellrate,
                        type = type,
                        profit = profit
                    )

                    appDao.insertVbstOut(vbstoutEntry)
                    count++
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing VBSTOUT CSV line: ${e.message}")
                }

                nextLine = reader.readNext()
            }

            try {
                val maxId = appDao.getMaxVbstOutId()
                if (maxId > 0) {
                    appDao.resetVbstOutSequenceTo(maxId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update VBSTOUT sequence: ${e.message}")
            }

            reader.close()
            Log.d(TAG, "Successfully imported $count VBSTOUT entries from CSV")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading VBSTOUT CSV data: ${e.message}")
        }
    }

    private suspend fun loadUsdtDataFromCsv(context: Context, appDao: AppDao) {
        try {
            val inputStream = context.assets.open(USDT_CSV_FILE)
            val reader = CSVReader(InputStreamReader(inputStream))

            // Skip header row if present
            var nextLine = reader.readNext()

            // Check if the first row is a header
            if (nextLine != null && nextLine[0].equals("id", ignoreCase = true)) {
                nextLine = reader.readNext()
            }

            var count = 0
            while (nextLine != null) {
                try {
                    val id = nextLine[0].toIntOrNull() ?: 0
                    val date = nextLine[1]
                    val person = nextLine[2]
                    val amountUsdt = nextLine[3].toDouble()
                    val amountCash = nextLine[4].toDouble()
                    val type = nextLine[5]

                    val usdtEntry = USDT(
                        id = id,
                        date = date,
                        person = person,
                        amountUsdt = amountUsdt,
                        amountCash = amountCash,
                        type = type
                    )

                    appDao.insertUsdt(usdtEntry)
                    count++
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing USDT CSV line: ${e.message}")
                }

                nextLine = reader.readNext()
            }

            try {
                val maxId = appDao.getMaxUsdtId()
                if (maxId > 0) {
                    appDao.resetUsdtSequenceTo(maxId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update USDT sequence: ${e.message}")
            }

            reader.close()
            Log.d(TAG, "Successfully imported $count USDT entries from CSV")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading USDT CSV data: ${e.message}")
        }
    }
}