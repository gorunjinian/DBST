package com.gorunjinian.dbst.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
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
import kotlin.reflect.KProperty1
import kotlinx.coroutines.withContext
import kotlin.reflect.full.memberProperties
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.gorunjinian.dbst.MyApplication.Companion.formatNumberWithCommas


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

        // Add click listener to show details
        adapter.onRecordClickListener = { record ->
            showRecordDetailsDialog(record)
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

        if (records.isEmpty()) {
            columnNames = emptyList()
            return
        }

        // Get the properties from the first record
        val firstRecord = records.first()
        val props = firstRecord::class.memberProperties.map { it.name }

        // Define the desired column order (same as we used for the data)
        val priorityOrder = listOf(
            "id",      // Keep ID first for reference
            "date",    // Date second
            "person",  // Person third

            // Amount variations
            "amount",
            "amountExpensed",
            "amountExchanged",
            "amountUsdt",
            "amountCash",

            // Rate variations
            "rate",
            "sellrate",

            // Type usually comes after amounts
            "type",
            "validity",

            // Calculated fields usually come last
            "totalLBP",
            "exchangedLBP",
            "profit",
            "total"
        )

        // Sort the property names based on the priority order
        val sortedProps = props.sortedBy { propName ->
            val index = priorityOrder.indexOf(propName)
            if (index >= 0) index else Int.MAX_VALUE
        }

        // Store the sorted column names for search functionality
        columnNames = sortedProps

        // Get entity type for display name mapping
        val entityType = when {
            records.first() is DBT -> "DBT"
            records.first() is DST -> "DST"
            records.first() is VBSTIN -> "VBSTIN"
            records.first() is VBSTOUT -> "VBSTOUT"
            records.first() is USDT -> "USDT"
            else -> ""
        }

        // Create and add header views in the sorted order
        sortedProps.forEach { propName ->
            // Get friendly display name based on entity type and property name
            val headerText = when (entityType) {
                "DBT" -> when (propName) {
                    "date" -> "Date"
                    "person" -> "Person"
                    "amount" -> "Amount"
                    "rate" -> "Rate"
                    "type" -> "Type"
                    "totalLBP" -> "Tot LBP"
                    else -> propName.replaceFirstChar { it.uppercaseChar() }
                }
                "DST" -> when (propName) {
                    "date" -> "Date"
                    "person" -> "Person"
                    "amountExpensed" -> "Expd"
                    "amountExchanged" -> "Exch"
                    "rate" -> "Rate"
                    "type" -> "Type"
                    "exchangedLBP" -> "Tot LBP"
                    else -> propName.replaceFirstChar { it.uppercaseChar() }
                }
                "VBSTIN" -> when (propName) {
                    "date" -> "Date"
                    "person" -> "Person"
                    "type" -> "Type"
                    "validity" -> "Validity"
                    "amount" -> "Amount"
                    "total" -> "Total"
                    "rate" -> "Rate"
                    else -> propName.replaceFirstChar { it.uppercaseChar() }
                }
                "VBSTOUT" -> when (propName) {
                    "date" -> "Date"
                    "person" -> "Person"
                    "amount" -> "Amount $"
                    "sellrate" -> "Sell R"
                    "type" -> "Type"
                    "profit" -> "Profit"
                    else -> propName.replaceFirstChar { it.uppercaseChar() }
                }
                "USDT" -> when (propName) {
                    "date" -> "Date"
                    "person" -> "Person"
                    "amountUsdt" -> "USDT"
                    "amountCash" -> "Cash"
                    "type" -> "Type"
                    else -> propName.replaceFirstChar { it.uppercaseChar() }
                }
                else -> propName.replaceFirstChar { it.uppercaseChar() }
            }

            val textView = TextView(requireContext()).apply {
                text = headerText
                textSize = 14f
                setTextColor(resources.getColor(R.color.white, requireContext().theme))
                setPadding(8, 8, 8, 8)
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            columnHeaderLayout.addView(textView)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
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
        val btnClear = dialogView.findViewById<MaterialButton>(R.id.btn_clear)

        // Completely disable dropdown auto-behavior
        columnSelector.isFocusable = false
        columnSelector.isClickable = true
        columnSelector.inputType = InputType.TYPE_NULL

        // Get column names from the current table
        val columnNames = when (currentTable) {
            "DBT" -> listOf("id", "date", "person", "amount", "rate", "type", "totalLBP")
            "DST" -> listOf("id", "date", "person", "amountExpensed", "amountExchanged", "rate", "type", "exchangedLBP")
            "VBSTIN" -> listOf("id", "date", "person", "type", "validity", "amount", "total", "rate")
            "VBSTOUT" -> listOf("id", "date", "person", "amount", "sellrate", "type", "profit")
            "USDT" -> listOf("id", "date", "person", "amountUsdt", "amountCash", "type")
            else -> emptyList()
        }

        // Set up adapter for the dropdown
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            columnNames
        )
        columnSelector.setAdapter(adapter)

        // Explicitly control dropdown behavior
        columnSelector.setOnClickListener {
            columnSelector.showDropDown()
        }

        // Handle item selection
        columnSelector.setOnItemClickListener { _, _, position, _ ->
            columnSelector.setText(columnNames[position], false)
            columnSelector.clearFocus()
        }

        // Completely prevent focus-related dropdown triggers
        columnSelector.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.clearFocus()
                columnSelector.dismissDropDown()
            }
        }

        // Modify dialog to intercept outside touches
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        // Explicitly handle dialog dismissal
        dialog.setOnCancelListener {
            columnSelector.dismissDropDown()
        }

        // Explicitly dismiss dropdown on cancel
        btnCancel.setOnClickListener {
            columnSelector.dismissDropDown()
            dialog.dismiss()
        }

        // Prevent dropdown from showing when dialog is about to dismiss
        dialog.window?.decorView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    columnSelector.dismissDropDown()
                    false // Allow normal dialog touch handling
                }
                else -> false
            }
        }

        // Rest of your existing dialog setup code...
        btnSearch.setOnClickListener {
            val selectedColumn = columnSelector.text.toString()
            val searchTerm = searchEditText.text.toString()

            if (selectedColumn.isNotEmpty() && searchTerm.isNotEmpty()) {
                performSearch(selectedColumn, searchTerm)
                (activity as? AppCompatActivity)?.supportActionBar?.title =
                    "Search: $selectedColumn='$searchTerm'"
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(),
                    "Please select a column and enter a search term",
                    Toast.LENGTH_SHORT).show()
            }
        }

        btnClear.setOnClickListener {
            columnSelector.text.clear()
            searchEditText.text?.clear()
            clearSearch()
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()

        // Ensure dropdown is dismissed after dialog is shown
        columnSelector.post {
            columnSelector.dismissDropDown()
        }
    }

    private fun showRecordDetailsDialog(record: Any) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_record_details)

        // Apply rounded background
        val background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_popup)
        dialog.window?.setBackgroundDrawable(background)

        // Set dialog width
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // Initialize views
        val detailsContainer = dialog.findViewById<LinearLayout>(R.id.details_container)
        val btnClose = dialog.findViewById<MaterialButton>(R.id.btn_close)
        val btnEdit = dialog.findViewById<MaterialButton>(R.id.btn_edit)

        // Populate details
        populateRecordDetails(detailsContainer, record)

        // Set button click listeners
        btnClose.setOnClickListener { dialog.dismiss() }
        btnEdit.setOnClickListener {
            dialog.dismiss()
            showEditRecordDialog(record)
        }

        dialog.show()
    }

    private fun populateRecordDetails(container: LinearLayout, record: Any) {
        container.removeAllViews()

        // Get properties using reflection with proper casting
        @Suppress("UNCHECKED_CAST")
        val props = record::class.memberProperties.toList() as List<KProperty1<Any, *>>

        // Sort properties in desired order
        val priorityOrder = listOf(
            "id", "date", "person",
            "amount", "amountExpensed", "amountExchanged", "amountUsdt", "amountCash",
            "rate", "sellrate",
            "type", "validity",
            "totalLBP", "exchangedLBP", "profit", "total"
        )

        val sortedProps = props.sortedBy { prop ->
            val index = priorityOrder.indexOf(prop.name)
            if (index >= 0) index else Int.MAX_VALUE
        }

        // Create layout for each property
        sortedProps.forEach { prop ->
            val fieldName = prop.name
            // This line now works with the proper casting
            val fieldValue = prop.get(record)?.toString() ?: ""

            // Get display name for the field
            val displayName = when (record) {
                is DBT -> when (fieldName) {
                    "date" -> "Date"
                    "person" -> "Person"
                    "amount" -> "Amount"
                    "rate" -> "Rate"
                    "type" -> "Type"
                    "totalLBP" -> "Total LBP"
                    else -> fieldName.replaceFirstChar { it.uppercase() }
                }
                is DST -> when (fieldName) {
                    "date" -> "Date"
                    "person" -> "Person"
                    "amountExpensed" -> "Amount Expensed"
                    "amountExchanged" -> "Amount Exchanged"
                    "rate" -> "Rate"
                    "type" -> "Type"
                    "exchangedLBP" -> "Exchanged LBP"
                    else -> fieldName.replaceFirstChar { it.uppercase() }
                }
                // Add cases for other entity types
                else -> fieldName.replaceFirstChar { it.uppercase() }
            }

            // Create field layout
            val fieldLayout = layoutInflater.inflate(R.layout.item_record_field, container, false)
            val labelView = fieldLayout.findViewById<TextView>(R.id.field_label)
            val valueView = fieldLayout.findViewById<TextView>(R.id.field_value)

            labelView.text = displayName
            valueView.text = fieldValue

            container.addView(fieldLayout)
        }
    }

    private fun showEditRecordDialog(record: Any) {
        // Determine record type and show appropriate edit dialog
        when (record) {
            is DBT -> showEditIncomeDialog(record)
            is DST -> showEditExpenseDialog(record)
//            is VBSTIN -> showEditVbstInDialog(record)
//            is VBSTOUT -> showEditVbstOutDialog(record)
//            is USDT -> showEditUsdtDialog(record)
            else -> Toast.makeText(requireContext(), "Editing not supported for this record type", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showEditExpenseDialog(record: DST) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_expense, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setTitle("Edit Expense Record")
            .create()

        // Initialize input fields
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.edit_date)
        val personInput = dialogView.findViewById<TextInputEditText>(R.id.edit_person)
        val amountExpensedInput = dialogView.findViewById<TextInputEditText>(R.id.edit_amount_expensed)
        val amountExchangedInput = dialogView.findViewById<TextInputEditText>(R.id.edit_amount_exchanged)
        val rateInput = dialogView.findViewById<TextInputEditText>(R.id.edit_rate)
        val typeInput = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.edit_type)

        formatNumberWithCommas(amountExpensedInput)
        formatNumberWithCommas(amountExchangedInput)
        formatNumberWithCommas(rateInput)

        // Set up type dropdown
        val typeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            listOf("FOOD", "GROCERIES", "EXCHANGE", "WELLBEING", "BANK TOPUP", "TECH", "DEBT", "OTHER")
        )
        typeInput.setAdapter(typeAdapter)

        // Fill fields with record data
        dateInput.setText(record.date)
        personInput.setText(record.person)
        amountExpensedInput.setText(record.amountExpensed.toString())
        amountExchangedInput.setText(record.amountExchanged.toString())
        rateInput.setText(record.rate.toString())
        typeInput.setText(record.type, false)

        // Set up buttons
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            // Validate inputs
            val date = dateInput.text.toString()
            val person = personInput.text.toString()
            val amountExpensedText = amountExpensedInput.text.toString()
            val amountExchangedText = amountExchangedInput.text.toString()
            val rateText = rateInput.text.toString()
            val type = typeInput.text.toString()

            if (date.isEmpty() || person.isEmpty() || amountExpensedText.isEmpty() || rateText.isEmpty() || type.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amountExpensed = amountExpensedText.toDoubleOrNull() ?: 0.0
            val amountExchanged = amountExchangedText.toDoubleOrNull() ?: 0.0
            val rate = rateText.toDoubleOrNull() ?: 0.0
            val exchangedLBP = amountExchanged * rate

            // Create updated record
            val updatedRecord = record.copy(
                date = date,
                person = person,
                amountExpensed = amountExpensed,
                amountExchanged = amountExchanged,
                rate = rate,
                type = type,
                exchangedLBP = exchangedLBP
            )

            // Save to database
            lifecycleScope.launch(Dispatchers.IO) {
                appDao.insertExpense(updatedRecord)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Expense record updated", Toast.LENGTH_SHORT).show()
                    loadTableData() // Refresh the data
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun showEditIncomeDialog(record: DBT) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_income, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setTitle("Edit Income Record")
            .create()

        // Initialize input fields
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.edit_date)
        val personInput = dialogView.findViewById<TextInputEditText>(R.id.edit_person)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.edit_amount)
        val rateInput = dialogView.findViewById<TextInputEditText>(R.id.edit_rate)
        val typeInput = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.edit_type)

        formatNumberWithCommas(amountInput)
        formatNumberWithCommas(rateInput)

        // Set up type dropdown
        val typeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            listOf("Income", "Buy", "Return", "Profit", "Validity", "Loan", "Gift", "Other", "N/A")
        )
        typeInput.setAdapter(typeAdapter)

        // Fill fields with record data
        dateInput.setText(record.date)
        personInput.setText(record.person)
        amountInput.setText(record.amount.toString())
        rateInput.setText(record.rate.toString())
        typeInput.setText(record.type, false)

        // Set up buttons
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            // Validate inputs
            val date = dateInput.text.toString()
            val person = personInput.text.toString()
            val amountText = amountInput.text.toString()
            val rateText = rateInput.text.toString()
            val type = typeInput.text.toString()

            if (date.isEmpty() || person.isEmpty() || amountText.isEmpty() || rateText.isEmpty() || type.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull() ?: 0.0
            val rate = rateText.toDoubleOrNull() ?: 0.0
            val totalLBP = amount * rate

            // Create updated record
            val updatedRecord = record.copy(
                date = date,
                person = person,
                amount = amount,
                rate = rate,
                type = type,
                totalLBP = totalLBP
            )

            // Save to database
            lifecycleScope.launch(Dispatchers.IO) {
                appDao.insertIncome(updatedRecord)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Record updated", Toast.LENGTH_SHORT).show()
                    loadTableData() // Refresh the data
                    dialog.dismiss()
                }
            }
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

    // method to clear search
    private fun clearSearch() {
        // Only proceed if there's an active search
        if (searchQuery != null && searchColumn != null) {
            searchQuery = null
            searchColumn = null

            // Reload the table without filters
            adapter.updateData(allRecords)

            // Reset the action bar title
            (activity as? AppCompatActivity)?.supportActionBar?.title = "Databases"

            Toast.makeText(requireContext(), "Search cleared", Toast.LENGTH_SHORT).show()
        }
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