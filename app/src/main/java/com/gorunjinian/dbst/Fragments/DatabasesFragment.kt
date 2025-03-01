package com.gorunjinian.dbst.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
import com.gorunjinian.dbst.data.DBT
import com.gorunjinian.dbst.data.DST
import com.gorunjinian.dbst.data.VBSTIN
import com.gorunjinian.dbst.data.VBSTOUT
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

        // Set long-press deletion callback
        adapter.onRecordLongClickListener = { record ->
            showDeleteConfirmationDialog(record)
        }

        // Load Table Names
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

        // Load last selected table or default to DST
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

            // Use reflection to obtain property names from the first record
            val props = firstRecord::class.memberProperties.toList()
            props.forEach { prop ->
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

    private fun showDeleteConfirmationDialog(record: Any) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Delete Record")
        builder.setMessage("Are you sure you want to delete this record?")
        builder.setPositiveButton("Delete") { dialog, _ ->
            lifecycleScope.launch(Dispatchers.IO) {
                when (record) {
                    is DBT -> appDao.deleteIncome(record.id)
                    is DST -> appDao.deleteExpense(record.id)
                    is VBSTIN -> appDao.deleteVbstIn(record.id)
                    is VBSTOUT -> appDao.deleteVbstOut(record.id)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Record deleted", Toast.LENGTH_SHORT).show()
                    loadTableData()
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }
}
