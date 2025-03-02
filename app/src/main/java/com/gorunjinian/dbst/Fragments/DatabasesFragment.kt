package com.gorunjinian.dbst.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import kotlin.reflect.full.memberProperties
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText


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

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu specifically for this fragment
        inflater.inflate(R.menu.search_menu, menu)

        // Get the search menu item and tint it to match toolbar title
        val searchItem = menu.findItem(R.id.action_search)
        searchItem?.icon?.setTint(resources.getColor(android.R.color.white, requireContext().theme))

        // Show or hide clear search option based on whether there's an active search
        // Added null check to prevent crashes if the item doesn't exist
        val clearItem = menu.findItem(R.id.action_clear_search)
        if (clearItem != null) {
            clearItem.isVisible = searchQuery != null && searchColumn != null
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                showSearchDialog()
                true
            }
            R.id.action_clear_search -> {
                clearSearch()
                activity?.invalidateOptionsMenu() // Refresh the options menu
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
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_search, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        // Get references to views
        val columnSelector = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.column_spinner)
        val searchEditText = dialogView.findViewById<TextInputEditText>(R.id.search_edit_text)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSearch = dialogView.findViewById<MaterialButton>(R.id.btn_search)
        val btnClear = dialogView.findViewById<MaterialButton>(R.id.btn_clear) // This ID matches your layout

        // Get column names from the current table
        val columnNames = when (currentTable) {
            "DBT" -> listOf("id", "date", "person", "amount", "rate", "type", "totalLBP")
            "DST" -> listOf("id", "date", "person", "amountExpensed", "amountExchanged", "rate", "type", "exchangedLBP")
            "VBSTIN" -> listOf("id", "date", "person", "type", "validity", "amount", "total", "rate")
            "VBSTOUT" -> listOf("id", "date", "person", "amount", "sellrate", "type", "profit")
            else -> emptyList()
        }

        // Set up adapter for the dropdown
        // With this built-in layout instead:
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, columnNames)
        columnSelector.setAdapter(adapter)

        // Make sure dropdown is properly configured
        columnSelector.threshold = 1  // Show dropdown after typing 1 character
        columnSelector.inputType = 0  // Set inputType to none to avoid keyboard
        columnSelector.setOnClickListener { columnSelector.showDropDown() }

        // Show or hide clear button based on active search
        btnClear.visibility = if (searchQuery != null && searchColumn != null) View.VISIBLE else View.GONE

        // Set first item selected by default if list is not empty
        if (!searchColumn.isNullOrEmpty() && columnNames.contains(searchColumn)) {
            // If we have an active search, pre-populate the fields
            columnSelector.setText(searchColumn, false)
            searchEditText.setText(searchQuery)
        } else if (columnNames.isNotEmpty()) {
            columnSelector.setText(columnNames[0], false)
        }

        // Set up button actions
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Add clear button click listener
        btnClear.setOnClickListener {
            clearSearch()
            dialog.dismiss()
        }

        btnSearch.setOnClickListener {
            val selectedColumn = columnSelector.text.toString()
            val searchTerm = searchEditText.text.toString()

            if (selectedColumn.isNotEmpty() && searchTerm.isNotEmpty()) {
                // Perform search based on selected column and search term
                performSearch(selectedColumn, searchTerm)

                // Update action bar title to show search info
                (activity as? AppCompatActivity)?.supportActionBar?.title =
                    "Search: $selectedColumn='$searchTerm'"

                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(),
                    "Please select a column and enter a search term",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // Also allow search when pressing search on keyboard
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val selectedColumn = columnSelector.text.toString()
                val searchTerm = searchEditText.text.toString()

                if (selectedColumn.isNotEmpty() && searchTerm.isNotEmpty()) {
                    performSearch(selectedColumn, searchTerm)

                    // Update action bar title to show search info
                    (activity as? AppCompatActivity)?.supportActionBar?.title =
                        "Search: $selectedColumn='$searchTerm'"

                    dialog.dismiss()
                    return@setOnEditorActionListener true
                } else {
                    Toast.makeText(requireContext(),
                        "Please select a column and enter a search term",
                        Toast.LENGTH_SHORT).show()
                }
            }
            false
        }

        dialog.show()
    }

    private fun loadTableData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val records = when (currentTable) {
                "DBT" -> appDao.getAllIncome()
                "DST" -> appDao.getAllExpense()
                "VBSTIN" -> appDao.getAllVbstIn()
                "VBSTOUT" -> appDao.getAllVbstOut()
                "USDT" -> {
                    // Check if this method exists in your AppDao
                    // If not, you need to add it or handle this case differently
                    try {
                        appDao.getAllUsdt()
                    } catch (e: Exception) {
                        Log.e("DatabasesFragment", "Error loading USDT data: ${e.message}")
                        emptyList<Any>()
                    }
                }
                else -> emptyList<Any>()
            }

            // Store all records for filtering
            allRecords = records

            withContext(Dispatchers.Main) {
                updateColumnHeaders(records)

                // Apply search filter if active
                if (!searchQuery.isNullOrEmpty() && !searchColumn.isNullOrEmpty() && records.isNotEmpty()) {
                    performSearch(searchColumn!!, searchQuery!!)
                } else {
                    adapter.updateData(records)
                }
            }
        }
    }

    // Update this method to store the search parameters
    private fun performSearch(column: String, searchTerm: String) {
        // Store current search parameters
        searchColumn = column
        searchQuery = searchTerm

        if (allRecords.isEmpty()) {
            Toast.makeText(requireContext(), "No data to search", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val results = when (currentTable) {
                "DBT" -> {
                    val allIncomeRecords = allRecords as List<*>
                    allIncomeRecords.filterIsInstance<DBT>().filter { record ->
                        when (column) {
                            "id" -> record.id.toString() == searchTerm
                            "date" -> record.date.contains(searchTerm, ignoreCase = true)
                            "person" -> record.person.contains(searchTerm, ignoreCase = true)
                            "amount" -> record.amount.toString().contains(searchTerm)
                            "rate" -> record.rate.toString().contains(searchTerm)
                            "type" -> record.type.contains(searchTerm, ignoreCase = true)
                            "totalLBP" -> record.totalLBP.toString().contains(searchTerm)
                            else -> false
                        }
                    }
                }
                "DST" -> {
                    val allExpenseRecords = allRecords as List<*>
                    allExpenseRecords.filterIsInstance<DST>().filter { record ->
                        when (column) {
                            "id" -> record.id.toString() == searchTerm
                            "date" -> record.date.contains(searchTerm, ignoreCase = true)
                            "person" -> record.person.contains(searchTerm, ignoreCase = true)
                            "amountExpensed" -> record.amountExpensed.toString().contains(searchTerm)
                            "amountExchanged" -> record.amountExchanged.toString().contains(searchTerm)
                            "rate" -> record.rate.toString().contains(searchTerm)
                            "type" -> record.type.contains(searchTerm, ignoreCase = true)
                            "exchangedLBP" -> record.exchangedLBP.toString().contains(searchTerm)
                            else -> false
                        }
                    }
                }
                "VBSTIN" -> {
                    val allVbstInRecords = allRecords as List<*>
                    allVbstInRecords.filterIsInstance<VBSTIN>().filter { record ->
                        when (column) {
                            "id" -> record.id.toString() == searchTerm
                            "date" -> record.date.contains(searchTerm, ignoreCase = true)
                            "person" -> record.person.contains(searchTerm, ignoreCase = true)
                            "type" -> record.type.contains(searchTerm, ignoreCase = true)
                            "validity" -> record.validity.contains(searchTerm, ignoreCase = true)
                            "amount" -> record.amount.toString().contains(searchTerm)
                            "total" -> record.total.toString().contains(searchTerm)
                            "rate" -> record.rate.toString().contains(searchTerm)
                            else -> false
                        }
                    }
                }
                "VBSTOUT" -> {
                    val allVbstOutRecords = allRecords as List<*>
                    allVbstOutRecords.filterIsInstance<VBSTOUT>().filter { record ->
                        when (column) {
                            "id" -> record.id.toString() == searchTerm
                            "date" -> record.date.contains(searchTerm, ignoreCase = true)
                            "person" -> record.person.contains(searchTerm, ignoreCase = true)
                            "amount" -> record.amount.toString().contains(searchTerm)
                            "sellrate" -> record.sellrate.toString().contains(searchTerm)
                            "type" -> record.type.contains(searchTerm, ignoreCase = true)
                            "profit" -> record.profit.toString().contains(searchTerm)
                            else -> false
                        }
                    }
                }
                "USDT" -> {
                    // Handle USDT entity if it exists
                    // This would use filterIsInstance<USDT> when you have that entity
                    emptyList<Any>()
                }
                else -> emptyList<Any>()
            }

            withContext(Dispatchers.Main) {
                adapter.updateData(results)
                if (results.isEmpty()) {
                    Toast.makeText(requireContext(), "No results found", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "${results.size} results found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Add method to clear search
    private fun clearSearch() {
        searchQuery = null
        searchColumn = null

        // Reload the table without filters
        adapter.updateData(allRecords)

        // Reset the action bar title
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Databases"

        Toast.makeText(requireContext(), "Search cleared", Toast.LENGTH_SHORT).show()
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