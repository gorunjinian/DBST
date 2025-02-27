package com.gorunjinian.dbst.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.db.SimpleSQLiteQuery
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.data.*
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.gorunjinian.dbst.adapters.DatabaseAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DatabasesFragment : Fragment() {

    private lateinit var database: EntryDatabase
    private lateinit var entryDao: EntryDao
    private lateinit var tableSpinner: MaterialAutoCompleteTextView
    private lateinit var tableSpinnerLayout: TextInputLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DatabaseAdapter
    private lateinit var columnHeaderLayout: TableRow
    private lateinit var importButton: Button

    private var availableTables: List<String> = emptyList()
    private var currentTable: String? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_databases, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Database
        database = EntryDatabase.getDatabase(requireContext())

        entryDao = database.entryDao()

        // UI Elements
        tableSpinnerLayout = view.findViewById(R.id.table_spinner_layout)
        tableSpinner = view.findViewById(R.id.table_spinner)
        recyclerView = view.findViewById(R.id.recycler_view)
        columnHeaderLayout = view.findViewById(R.id.table_header)
        importButton = view.findViewById(R.id.import_button)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = DatabaseAdapter()
        recyclerView.adapter = adapter


        // Load Table Names
        loadTableNames()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
        }

    }

    private fun loadTableNames() {
        lifecycleScope.launch(Dispatchers.IO) {
            // ✅ Execute raw SQL query to get table names, excluding system tables
            val rawQuery = SimpleSQLiteQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_metadata' AND name NOT LIKE 'sqlite_sequence' AND name NOT LIKE 'room_master_table'"
            )
            availableTables = entryDao.getAllTableNames(rawQuery)

            withContext(Dispatchers.Main) {
                if (availableTables.isEmpty()) {
                    Toast.makeText(requireContext(), "No tables found!", Toast.LENGTH_SHORT).show()
                } else {
                    setupTableSpinner()
                }
            }
        }
    }

    private fun setupTableSpinner() {
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, availableTables)
        tableSpinner.setAdapter(spinnerAdapter)

        tableSpinner.setOnItemClickListener { _, _, position, _ ->
            currentTable = availableTables[position]
            loadTableData()
        }
    }

    private fun loadTableData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val records = when (currentTable) {
                "DBT" -> entryDao.getAllIncome()
                "DST" -> entryDao.getAllExpense()
                else -> emptyList()
            }

            withContext(Dispatchers.Main) {
                updateColumnHeaders()
                adapter.updateData(currentTable ?: "", records)
            }
        }
    }

    private fun updateColumnHeaders() {
        columnHeaderLayout.removeAllViews()

        val headers = when (currentTable) {
            "DBT" -> listOf("Date", "Person", "Amount", "Rate", "Type", "Total LBP")
            "DST" -> listOf("Date", "Person", "Expd", "Exch", "Rate", "Type", "Exchanged LBP")
            else -> return
        }

        headers.forEach { title ->
            val textView = TextView(requireContext()).apply {
                text = title
                textSize = 14f
                setTextColor(resources.getColor(R.color.white, requireContext().theme))
                setPadding(8, 8, 8, 8)
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
            }
            columnHeaderLayout.addView(textView)
        }
    }

}
