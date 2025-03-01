package com.gorunjinian.dbst.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.data.AppDao
import com.gorunjinian.dbst.data.AppDatabase
import com.gorunjinian.dbst.data.DatabaseAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.full.memberProperties

class DatabasesFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var appDao: AppDao
    private lateinit var tableSpinner: MaterialAutoCompleteTextView
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

        // Initialize Database and DAO
        database = AppDatabase.getDatabase(requireContext())
        appDao = database.appDao()

        // UI Elements
        tableSpinner = view.findViewById(R.id.table_spinner)
        recyclerView = view.findViewById(R.id.recycler_view)
        columnHeaderLayout = view.findViewById(R.id.table_header)
        importButton = view.findViewById(R.id.import_button)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        )
        adapter = DatabaseAdapter()
        recyclerView.adapter = adapter

        // Load Table Names from the SQLite master table
        loadTableNames()
    }

    private fun loadTableNames() {
        lifecycleScope.launch(Dispatchers.IO) {
            val rawQuery = SimpleSQLiteQuery(
                "SELECT name FROM sqlite_master WHERE type='table' " +
                        "AND name NOT LIKE 'android_metadata' " +
                        "AND name NOT LIKE 'sqlite_sequence' " +
                        "AND name NOT LIKE 'room_master_table'"
            )
            availableTables = appDao.getAllTableNames(rawQuery)
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

        // Load last selected table or default to DST (modify the default as needed)
        val prefs = requireActivity().getSharedPreferences("db_prefs", Context.MODE_PRIVATE)
        currentTable = prefs.getString("last_table", "DST")
        tableSpinner.setText(currentTable, false)
        loadTableData()

        tableSpinner.setOnItemClickListener { _, _, position, _ ->
            currentTable = availableTables[position]
            prefs.edit().putString("last_table", currentTable).apply()
            loadTableData()
        }
    }

    private fun loadTableData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val records = when (currentTable) {
                "DBT" -> appDao.getAllIncome()
                "DST" -> appDao.getAllExpense()
                "VBSTIN" -> appDao.getAllVbstIn()
                "VBSTOUT" -> appDao.getAllVbstOut()
                else -> emptyList()
            }
            withContext(Dispatchers.Main) {
                updateColumnHeaders(records)
                adapter.updateData(records)
            }
        }
    }

    private fun updateColumnHeaders(records: List<Any>) {
        columnHeaderLayout.removeAllViews()

        if (records.isNotEmpty()) {
            val firstRecord = records.first()
            // Use reflection to get property names from the first record
            val props = firstRecord::class.memberProperties.toList()
            props.forEach { prop ->
                // Capitalize first letter of the property name
                val headerText = prop.name.replaceFirstChar { it.uppercaseChar() }
                val textView = TextView(requireContext()).apply {
                    text = headerText
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
}
