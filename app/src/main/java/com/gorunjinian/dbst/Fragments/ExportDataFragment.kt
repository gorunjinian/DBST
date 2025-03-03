package com.gorunjinian.dbst.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.data.AppDao
import com.gorunjinian.dbst.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportDataFragment : Fragment() {

    // Database components
    private lateinit var database: AppDatabase
    private lateinit var appDao: AppDao

    // UI Components
    private lateinit var checkboxIncome: CheckBox
    private lateinit var checkboxExpense: CheckBox
    private lateinit var checkboxValidityIn: CheckBox
    private lateinit var checkboxValidityOut: CheckBox
    private lateinit var checkboxUsdt: CheckBox
    private lateinit var radioCsv: RadioButton
    private lateinit var radioJson: RadioButton
    private lateinit var exportButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var exportStatusText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_export_data, container, false)
        Log.d("FragmentDebug", "ExportDataFragment UI Inflated")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FragmentDebug", "View created successfully!")

        // Initialize Database and DAO
        database = AppDatabase.getDatabase(requireContext())
        appDao = database.appDao()

        // Initialize UI components
        initializeUI(view)

        // Set up click listener for export button
        exportButton.setOnClickListener {
            exportData()
        }
    }

    private fun initializeUI(view: View) {
        checkboxIncome = view.findViewById(R.id.checkbox_income)
        checkboxExpense = view.findViewById(R.id.checkbox_expense)
        checkboxValidityIn = view.findViewById(R.id.checkbox_validity_in)
        checkboxValidityOut = view.findViewById(R.id.checkbox_validity_out)
        checkboxUsdt = view.findViewById(R.id.checkbox_usdt)
        radioCsv = view.findViewById(R.id.radio_csv)
        radioJson = view.findViewById(R.id.radio_json)
        exportButton = view.findViewById(R.id.export_button)
        progressBar = view.findViewById(R.id.progress_bar)
        exportStatusText = view.findViewById(R.id.export_status_text)
    }

    @SuppressLint("SetTextI18n")
    private fun exportData() {
        // Check if at least one checkbox is selected
        if (!checkboxIncome.isChecked && !checkboxExpense.isChecked &&
            !checkboxValidityIn.isChecked && !checkboxValidityOut.isChecked &&
            !checkboxUsdt.isChecked
        ) {
            Toast.makeText(
                requireContext(),
                "Please select at least one data type to export",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Show progress and start export process
        progressBar.visibility = View.VISIBLE
        exportStatusText.text = "Exporting data..."
        exportButton.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(Date())
                val fileName = "DBST_Export_$timestamp.${if (radioCsv.isChecked) "csv" else "json"}"

                // Get the directory for app-specific files
                val exportDir =
                    File(
                        requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                        "DBST_Exports"
                    ).apply { mkdirs() }

                val exportFile = File(exportDir, fileName)
                val fileWriter = FileWriter(exportFile)

                // Export based on selected format
                if (radioCsv.isChecked) {
                    exportToCsv(fileWriter)
                } else {
                    exportToJson(fileWriter)
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    exportStatusText.text = "Export successful: ${exportFile.name}"
                    exportButton.isEnabled = true

                    // Offer to share the file
                    shareFile(exportFile)
                }
            } catch (e: Exception) {
                Log.e("ExportDataFragment", "Export error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    exportStatusText.text = "Export failed: ${e.message}"
                    exportButton.isEnabled = true
                }
            }
        }
    }

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = if (radioCsv.isChecked) "text/csv" else "application/json"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share exported data"))
        } catch (e: Exception) {
            Log.e("ExportDataFragment", "Error sharing file: ${e.message}", e)
            Toast.makeText(
                requireContext(),
                "Unable to share file: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private suspend fun exportToCsv(fileWriter: FileWriter) {
        try {
            // Export DBT (Income) data if selected
            if (checkboxIncome.isChecked) {
                val incomeEntries = appDao.getAllIncome()
                withContext(Dispatchers.IO) {
                    fileWriter.append("=== INCOME DATA (DBT) ===\n")
                }
                withContext(Dispatchers.IO) {
                    fileWriter.append("ID,Date,Person,Amount,Rate,Type,TotalLBP\n")
                }

                for (entry in incomeEntries) {
                    withContext(Dispatchers.IO) {
                        fileWriter.append("${entry.id},")
                    }
                    withContext(Dispatchers.IO) {
                        fileWriter.append("\"${entry.date}\",")
                    }
                    withContext(Dispatchers.IO) {
                        fileWriter.append("\"${entry.person.replace("\"", "\"\"")}\",")
                    } // Escape quotes
                    withContext(Dispatchers.IO) {
                        fileWriter.append("${entry.amount},")
                    }
                    withContext(Dispatchers.IO) {
                        fileWriter.append("${entry.rate},")
                    }
                    withContext(Dispatchers.IO) {
                        fileWriter.append("\"${entry.type.replace("\"", "\"\"")}\",")
                    } // Escape quotes
                    withContext(Dispatchers.IO) {
                        fileWriter.append("${entry.totalLBP}\n")
                    }
                }
                withContext(Dispatchers.IO) {
                    fileWriter.append("\n")
                }
            }

            // Export DST (Expense) data if selected
            if (checkboxExpense.isChecked) {
                val expenseEntries = appDao.getAllExpense()
                withContext(Dispatchers.IO) {
                    fileWriter.append("=== EXPENSE DATA (DST) ===\n")
                }
                withContext(Dispatchers.IO) {
                    fileWriter.append("ID,Date,Person,AmountExpensed,AmountExchanged,Rate,Type,ExchangedLBP\n")
                }

                for (entry in expenseEntries) {
                    withContext(Dispatchers.IO) {
                        fileWriter.append("${entry.id},")
                    }
                    withContext(Dispatchers.IO) {
                        fileWriter.append("\"${entry.date}\",")
                    }
                    fileWriter.append("\"${entry.person.replace("\"", "\"\"")}\",")
                    fileWriter.append("${entry.amountExpensed},")
                    fileWriter.append("${entry.amountExchanged},")
                    fileWriter.append("${entry.rate},")
                    fileWriter.append("\"${entry.type.replace("\"", "\"\"")}\",")
                    fileWriter.append("${entry.exchangedLBP}\n")
                }
                fileWriter.append("\n")
            }

            // Export VBSTIN data if selected
            if (checkboxValidityIn.isChecked) {
                val vbstInEntries = appDao.getAllVbstIn()
                withContext(Dispatchers.IO) {
                    fileWriter.append("=== VALIDITY IN DATA (VBSTIN) ===\n")
                }
                fileWriter.append("ID,Date,Person,Type,Validity,Amount,Total,Rate\n")

                for (entry in vbstInEntries) {
                    fileWriter.append("${entry.id},")
                    fileWriter.append("\"${entry.date}\",")
                    fileWriter.append("\"${entry.person.replace("\"", "\"\"")}\",")
                    fileWriter.append("\"${entry.type.replace("\"", "\"\"")}\",")
                    fileWriter.append("\"${entry.validity}\",")
                    fileWriter.append("${entry.amount},")
                    fileWriter.append("${entry.total},")
                    fileWriter.append("${entry.rate}\n")
                }
                fileWriter.append("\n")
            }

            // Export VBSTOUT data if selected
            if (checkboxValidityOut.isChecked) {
                val vbstOutEntries = appDao.getAllVbstOut()
                fileWriter.append("=== VALIDITY OUT DATA (VBSTOUT) ===\n")
                fileWriter.append("ID,Date,Person,Amount,SellRate,Type,Profit\n")

                for (entry in vbstOutEntries) {
                    fileWriter.append("${entry.id},")
                    fileWriter.append("\"${entry.date}\",")
                    fileWriter.append("\"${entry.person.replace("\"", "\"\"")}\",")
                    fileWriter.append("${entry.amount},")
                    fileWriter.append("${entry.sellrate},")
                    fileWriter.append("\"${entry.type.replace("\"", "\"\"")}\",")
                    fileWriter.append("${entry.profit}\n")
                }
                fileWriter.append("\n")
            }

            // Export USDT data if selected
            if (checkboxUsdt.isChecked) {
                val usdtEntries = appDao.getAllUsdt()
                fileWriter.append("=== USDT DATA ===\n")
                fileWriter.append("ID,Date,Person,AmountUsdt,AmountCash,Type\n")

                for (entry in usdtEntries) {
                    fileWriter.append("${entry.id},")
                    fileWriter.append("\"${entry.date}\",")
                    fileWriter.append("\"${entry.person.replace("\"", "\"\"")}\",")
                    fileWriter.append("${entry.amountUsdt},")
                    fileWriter.append("${entry.amountCash},")
                    fileWriter.append("\"${entry.type.replace("\"", "\"\"")}\"\n")
                }
            }

            fileWriter.flush()
            fileWriter.close()
        } catch (e: Exception) {
            Log.e("ExportDataFragment", "CSV Export error: ${e.message}", e)
            throw e
        }
    }

    private suspend fun exportToJson(fileWriter: FileWriter) {
        try {
            val rootJson = JSONObject()

            // Export DBT (Income) data if selected
            if (checkboxIncome.isChecked) {
                val incomeEntries = appDao.getAllIncome()
                val incomeArray = JSONArray()

                for (entry in incomeEntries) {
                    val entryJson = JSONObject().apply {
                        put("id", entry.id)
                        put("date", entry.date)
                        put("person", entry.person)
                        put("amount", entry.amount)
                        put("rate", entry.rate)
                        put("type", entry.type)
                        put("totalLBP", entry.totalLBP)
                    }
                    incomeArray.put(entryJson)
                }
                rootJson.put("income_data", incomeArray)
            }

            // Export DST (Expense) data if selected
            if (checkboxExpense.isChecked) {
                val expenseEntries = appDao.getAllExpense()
                val expenseArray = JSONArray()

                for (entry in expenseEntries) {
                    val entryJson = JSONObject().apply {
                        put("id", entry.id)
                        put("date", entry.date)
                        put("person", entry.person)
                        put("amountExpensed", entry.amountExpensed)
                        put("amountExchanged", entry.amountExchanged)
                        put("rate", entry.rate)
                        put("type", entry.type)
                        put("exchangedLBP", entry.exchangedLBP)
                    }
                    expenseArray.put(entryJson)
                }
                rootJson.put("expense_data", expenseArray)
            }

            // Export VBSTIN data if selected
            if (checkboxValidityIn.isChecked) {
                val vbstInEntries = appDao.getAllVbstIn()
                val vbstInArray = JSONArray()

                for (entry in vbstInEntries) {
                    val entryJson = JSONObject().apply {
                        put("id", entry.id)
                        put("date", entry.date)
                        put("person", entry.person)
                        put("type", entry.type)
                        put("validity", entry.validity)
                        put("amount", entry.amount)
                        put("total", entry.total)
                        put("rate", entry.rate)
                    }
                    vbstInArray.put(entryJson)
                }
                rootJson.put("validity_in_data", vbstInArray)
            }

            // Export VBSTOUT data if selected
            if (checkboxValidityOut.isChecked) {
                val vbstOutEntries = appDao.getAllVbstOut()
                val vbstOutArray = JSONArray()

                for (entry in vbstOutEntries) {
                    val entryJson = JSONObject().apply {
                        put("id", entry.id)
                        put("date", entry.date)
                        put("person", entry.person)
                        put("amount", entry.amount)
                        put("sellrate", entry.sellrate)
                        put("type", entry.type)
                        put("profit", entry.profit)
                    }
                    vbstOutArray.put(entryJson)
                }
                rootJson.put("validity_out_data", vbstOutArray)
            }

            // Export USDT data if selected
            if (checkboxUsdt.isChecked) {
                val usdtEntries = appDao.getAllUsdt()
                val usdtArray = JSONArray()

                for (entry in usdtEntries) {
                    val entryJson = JSONObject().apply {
                        put("id", entry.id)
                        put("date", entry.date)
                        put("person", entry.person)
                        put("amountUsdt", entry.amountUsdt)
                        put("amountCash", entry.amountCash)
                        put("type", entry.type)
                    }
                    usdtArray.put(entryJson)
                }
                rootJson.put("usdt_data", usdtArray)
            }

            // Add summary information
            val summaryJson = calculateFinancialSummary()
            rootJson.put("financial_summary", summaryJson)

            fileWriter.write(rootJson.toString(4))  // Pretty print with 4-space indentation
            fileWriter.flush()
            fileWriter.close()
        } catch (e: Exception) {
            Log.e("ExportDataFragment", "JSON Export error: ${e.message}", e)
            throw e
        }
    }

    private suspend fun calculateFinancialSummary(): JSONObject {
        val summaryJson = JSONObject()

        // Total Income
        val incomeEntries = appDao.getAllIncome()
        val totalIncome = incomeEntries.sumOf { it.totalLBP }
        summaryJson.put("total_income_lbp", totalIncome)

        // Total Expense
        val expenseEntries = appDao.getAllExpense()
        val totalExpense = expenseEntries.sumOf { it.amountExpensed }
        val totalExchanged = expenseEntries.sumOf { it.exchangedLBP }
        summaryJson.put("total_expense", totalExpense)
        summaryJson.put("total_exchanged_lbp", totalExchanged)

        // Calculate balance
        val balance = totalIncome - totalExchanged
        summaryJson.put("balance_lbp", balance)

        // Expense categories breakdown
        val expenseCategories = expenseEntries.groupBy { it.type }
        val categoryBreakdown = JSONObject()
        expenseCategories.forEach { (category, entries) ->
            categoryBreakdown.put(category, entries.sumOf { it.amountExpensed })
        }
        summaryJson.put("expense_categories", categoryBreakdown)

        return summaryJson
    }
}