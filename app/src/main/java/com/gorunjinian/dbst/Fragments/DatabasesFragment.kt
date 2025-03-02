package com.gorunjinian.dbst.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KProperty1
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

    // Search related variables
    private var allRecords: List<Any> = emptyList()
    private var columnNames: List<String> = emptyList()
    private var searchColumn: String? = null
    private var searchQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable options menu in this fragment
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_databases, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu specifically for this fragment
        inflater.inflate(R.menu.search_menu, menu)

        // Get the search menu item and tint it to match toolbar title
        val searchItem = menu.findItem(R.id.action_search)
        searchItem.icon?.setTint(resources.getColor(android.R.color.white, requireContext().theme))

        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                showSearchDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

            // Reset search when changing tables
            searchColumn = null
            searchQuery = null
            (activity as? AppCompatActivity)?.supportActionBar?.title = "Databases"

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
                "USDT" -> appDao.getAllUsdt()
                else -> emptyList()
            }

            // Store all records for filtering
            allRecords = records

            withContext(Dispatchers.Main) {
                updateColumnHeaders(records)

                // Apply search filter if active
                if (searchQuery != null && searchColumn != null && records.isNotEmpty()) {
                    applySearchFilter()
                } else {
                    adapter.updateData(records)
                }
            }
        }
    }

    private fun updateColumnHeaders(records: List<Any>) {
        columnHeaderLayout.removeAllViews()
        columnNames = emptyList()

        if (records.isNotEmpty()) {
            val firstRecord = records.first()

            // Use reflection to obtain property names from the first record
            val props = firstRecord::class.memberProperties.toList()
            columnNames = props.map { it.name }

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

    private fun showSearchDialog() {
        if (columnNames.isEmpty()) {
            Toast.makeText(requireContext(), "No data available to search", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_search, null)
        val columnSpinner = dialogView.findViewById<Spinner>(R.id.column_spinner)
        val searchEditText = dialogView.findViewById<EditText>(R.id.search_edit_text)

        // Set up column spinner
        val columnAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            columnNames.map { it.replaceFirstChar { char -> char.uppercaseChar() } }
        )
        columnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        columnSpinner.adapter = columnAdapter

        // Select previously chosen column if available
        searchColumn?.let { column ->
            val columnIndex = columnNames.indexOf(column)
            if (columnIndex >= 0) {
                columnSpinner.setSelection(columnIndex)
            }
        }

        // Fill in previous search query if available
        searchQuery?.let { query ->
            searchEditText.setText(query)
        }

        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Search ${currentTable ?: "Records"}")
            .setView(dialogView)
            .setPositiveButton("Search") { _, _ ->
                val selectedColumnIndex = columnSpinner.selectedItemPosition
                if (selectedColumnIndex >= 0) {
                    searchColumn = columnNames[selectedColumnIndex]
                    searchQuery = searchEditText.text.toString().trim()

                    if (searchQuery.isNullOrEmpty()) {
                        Toast.makeText(requireContext(), "Please enter a search term", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    applySearchFilter()
                }
            }
            .setNegativeButton("Clear") { _, _ ->
                searchColumn = null
                searchQuery = null
                adapter.updateData(allRecords)

                // Update title to show search is cleared
                (activity as? AppCompatActivity)?.supportActionBar?.title = "Databases"
            }
            .setNeutralButton("Cancel", null)

        builder.create().show()
    }

    private fun applySearchFilter() {
        if (searchColumn == null || searchQuery == null || allRecords.isEmpty()) {
            return
        }

        val filteredRecords = allRecords.filter { record ->
            // Get the specified property using reflection
            val property = record::class.memberProperties.find { it.name == searchColumn }

            if (property != null) {
                // Get property value as string
                val value = (property as KProperty1<Any, *>).get(record)
                // Case-insensitive string contains
                return@filter value.toString().contains(searchQuery!!, ignoreCase = true)
            }

            false
        }

        adapter.updateData(filteredRecords)

        // Update title to indicate search is active
        (activity as? AppCompatActivity)?.supportActionBar?.title =
            "Databases (${filteredRecords.size}/${allRecords.size})"
    }


    private fun showDeleteConfirmationDialog(record: Any) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Delete Record")
        builder.setMessage("Are you sure you want to delete this record?")
        builder.setPositiveButton("Delete") { dialog, _ ->
            lifecycleScope.launch(Dispatchers.IO) {
                when (record) {
                    is DBT -> {
                        appDao.deleteIncome(record.id)
                        // Only reset sequence if all records are deleted
                        val remainingRecords = appDao.getAllIncome()
                        if (remainingRecords.isEmpty()) {
                            appDao.resetDbtSequence()
                        }
                    }
                    is DST -> {
                        appDao.deleteExpense(record.id)
                        // Only reset sequence if all records are deleted
                        val remainingRecords = appDao.getAllExpense()
                        if (remainingRecords.isEmpty()) {
                            appDao.resetDstSequence()
                        }
                    }
                    is VBSTIN -> {
                        appDao.deleteVbstIn(record.id)
                        // Only reset sequence if all records are deleted
                        val remainingRecords = appDao.getAllVbstIn()
                        if (remainingRecords.isEmpty()) {
                            appDao.resetVbstInSequence()
                        }
                    }
                    is VBSTOUT -> {
                        appDao.deleteVbstOut(record.id)
                        // Only reset sequence if all records are deleted
                        val remainingRecords = appDao.getAllVbstOut()
                        if (remainingRecords.isEmpty()) {
                            appDao.resetVbstOutSequence()
                        }
                    }
                    is USDT -> {
                        appDao.deleteUsdt(record.id)
                        val remainingRecords = appDao.getAllUsdt()
                        if (remainingRecords.isEmpty()) {
                            appDao.resetUsdtSequence()
                        }
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "Unsupported record type",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }
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