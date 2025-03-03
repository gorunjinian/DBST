package com.gorunjinian.dbst.fragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.gorunjinian.dbst.MyApplication.Companion.formatNumberWithCommas
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
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
        inflater.inflate(R.menu.search_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        searchItem?.icon?.setTint(resources.getColor(android.R.color.white, requireContext().theme))

        val clearItem = menu.findItem(R.id.action_clear_search)
        clearItem?.isVisible = searchQuery != null && searchColumn != null

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
                activity?.invalidateOptionsMenu()
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

        // Set callbacks for record actions
        setupAdapterCallbacks()

        // Load Table Names
        loadTableNames()
    }

    private fun setupAdapterCallbacks() {
        // Setup long-press deletion
        adapter.onRecordLongClickListener = { record ->
            showDeleteConfirmationDialog(record)
        }

        // Setup click to view details
        adapter.onRecordClickListener = { record ->
            showRecordDetailsDialog(record)
        }
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
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            availableTables
        )
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

        // Define the desired column order
        val priorityOrder = listOf(
            "id", "date", "person", "amount", "amountExpensed", "amountExchanged",
            "amountUsdt", "amountCash", "rate", "sellrate", "type", "validity",
            "totalLBP", "exchangedLBP", "profit", "total"
        )

        // Sort the property names based on the priority order
        val sortedProps = props.sortedBy { propName ->
            val index = priorityOrder.indexOf(propName)
            if (index >= 0) index else Int.MAX_VALUE
        }

        // Store the sorted column names for search functionality
        columnNames = sortedProps

        // Create and add header views in the sorted order
        sortedProps.forEach { propName ->
            // Use adapter's method to get the display name
            val headerText = adapter.getDisplayNameForProperty(propName)
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

        // Improve dropdown behavior
        configureSearchDropdown(columnSelector, dialog)

        // Get column names for the current table
        val columnNames = getColumnNamesForCurrentTable()

        // Set up adapter for the dropdown
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            columnNames
        )
        columnSelector.setAdapter(adapter)

        // Populate search field if we have an active search
        if (!searchColumn.isNullOrEmpty() && !searchQuery.isNullOrEmpty()) {
            columnSelector.setText(searchColumn, false)
            searchEditText.setText(searchQuery)
        }

        // Set up button actions
        btnSearch.setOnClickListener {
            val selectedColumn = columnSelector.text.toString()
            val searchTerm = searchEditText.text.toString()

            if (selectedColumn.isNotEmpty() && searchTerm.isNotEmpty()) {
                performSearch(selectedColumn, searchTerm)
                (activity as? AppCompatActivity)?.supportActionBar?.title =
                    "Search: $selectedColumn='$searchTerm'"
                dialog.dismiss()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please select a column and enter a search term",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnClear.setOnClickListener {
            clearSearch()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configureSearchDropdown(
        columnSelector: MaterialAutoCompleteTextView,
        dialog: AlertDialog
    ) {
        // Disable automatic focus behavior
        columnSelector.isFocusable = false
        columnSelector.isClickable = true
        columnSelector.inputType = InputType.TYPE_NULL

        // Show dropdown explicitly on click
        columnSelector.setOnClickListener {
            columnSelector.showDropDown()
        }

        // Handle selection
        columnSelector.setOnItemClickListener { _, _, _, _ ->
            columnSelector.clearFocus()
        }

        // Prevent focus-related dropdown triggers
        columnSelector.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.clearFocus()
                columnSelector.dismissDropDown()
            }
        }

        // Dismiss dropdown on dialog cancel
        dialog.setOnCancelListener {
            columnSelector.dismissDropDown()
        }

        // Handle dialog touch events
        dialog.window?.decorView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                columnSelector.dismissDropDown()
            }
            false
        }

        // Ensure dropdown is dismissed after dialog is shown
        columnSelector.post {
            columnSelector.dismissDropDown()
        }
    }

    private fun getColumnNamesForCurrentTable(): List<String> {
        return when (currentTable) {
            "DBT" -> listOf("id", "date", "person", "amount", "rate", "type", "totalLBP")
            "DST" -> listOf(
                "id", "date", "person", "amountExpensed", "amountExchanged",
                "rate", "type", "exchangedLBP"
            )
            "VBSTIN" -> listOf("id", "date", "person", "type", "validity", "amount", "total", "rate")
            "VBSTOUT" -> listOf("id", "date", "person", "amount", "sellrate", "type", "profit")
            "USDT" -> listOf("id", "date", "person", "amountUsdt", "amountCash", "type")
            else -> emptyList()
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
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

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

        // Define priority order for properties
        val priorityOrder = listOf(
            "id", "date", "person", "amount", "amountExpensed", "amountExchanged",
            "amountUsdt", "amountCash", "rate", "sellrate", "type", "validity",
            "totalLBP", "exchangedLBP", "profit", "total"
        )

        // Sort properties
        val sortedProps = props.sortedBy { prop ->
            val index = priorityOrder.indexOf(prop.name)
            if (index >= 0) index else Int.MAX_VALUE
        }

        // Get entity type for the adapter
        val entityType = when (record) {
            is DBT -> "DBT"
            is DST -> "DST"
            is VBSTIN -> "VBSTIN"
            is VBSTOUT -> "VBSTOUT"
            is USDT -> "USDT"
            else -> ""
        }

        // Use adapter to get display names
        val tempAdapter = DatabaseAdapter().apply {
            updateData(listOf(record))
        }

        // Create layout for each property
        sortedProps.forEach { prop ->
            val fieldName = prop.name
            val fieldValue = prop.get(record)?.toString() ?: ""

            // Get display name for the field using the adapter
            val displayName = tempAdapter.getDisplayNameForProperty(fieldName)

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
        when (record) {
            is DBT -> showEditIncomeDialog(record)
            is DST -> showEditExpenseDialog(record)
            is VBSTIN -> showEditVbstInDialog(record)
            is VBSTOUT -> showEditVbstOutDialog(record)
            is USDT -> showEditUsdtDialog(record)
            else -> Toast.makeText(
                requireContext(),
                "Editing not supported for this record type",
                Toast.LENGTH_SHORT
            ).show()
        }
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
        typeInput.setOnClickListener { typeInput.showDropDown() }

        // Set up date picker
        setupDatePicker(dateInput)

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
            if (validateAndSaveIncome(record, dateInput, personInput, amountInput, rateInput, typeInput)) {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun validateAndSaveIncome(
        record: DBT,
        dateInput: TextInputEditText,
        personInput: TextInputEditText,
        amountInput: TextInputEditText,
        rateInput: TextInputEditText,
        typeInput: MaterialAutoCompleteTextView
    ): Boolean {
        // Get values
        val date = dateInput.text.toString()
        val person = personInput.text.toString()
        val amountText = amountInput.text.toString()
        val rateText = rateInput.text.toString()
        val type = typeInput.text.toString()

        // Validate inputs
        if (date.isEmpty() || person.isEmpty() || amountText.isEmpty() || rateText.isEmpty() || type.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            // Parse values
            val amount = amountText.replace(",", "").toDouble()
            val rate = rateText.replace(",", "").toDouble()
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
                    Toast.makeText(requireContext(), "Income record updated", Toast.LENGTH_SHORT).show()
                    loadTableData()
                }
            }
            return true
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Invalid number format", Toast.LENGTH_SHORT).show()
            return false
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
        typeInput.setOnClickListener { typeInput.showDropDown() }

        // Set up date picker
        setupDatePicker(dateInput)

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
            if (validateAndSaveExpense(record, dateInput, personInput, amountExpensedInput,
                    amountExchangedInput, rateInput, typeInput)) {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun validateAndSaveExpense(
        record: DST,
        dateInput: TextInputEditText,
        personInput: TextInputEditText,
        amountExpensedInput: TextInputEditText,
        amountExchangedInput: TextInputEditText,
        rateInput: TextInputEditText,
        typeInput: MaterialAutoCompleteTextView
    ): Boolean {
        // Get values
        val date = dateInput.text.toString()
        val person = personInput.text.toString()
        val amountExpensedText = amountExpensedInput.text.toString()
        val amountExchangedText = amountExchangedInput.text.toString()
        val rateText = rateInput.text.toString()
        val type = typeInput.text.toString()

        // Validate inputs
        if (date.isEmpty() || person.isEmpty() || amountExpensedText.isEmpty() ||
            rateText.isEmpty() || type.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            // Parse values
            val amountExpensed = amountExpensedText.replace(",", "").toDouble()
            val amountExchanged = if (amountExchangedText.isNotEmpty())
                amountExchangedText.replace(",", "").toDouble() else 0.0
            val rate = rateText.replace(",", "").toDouble()
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
                    loadTableData()
                }
            }
            return true
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Invalid number format", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    @SuppressLint("SetTextI18s", "SetTextI18n")
    private fun showEditVbstInDialog(record: VBSTIN) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_vbstin, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        // Get references to all views
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.date_input)
        val personInput = dialogView.findViewById<TextInputEditText>(R.id.person_input)
        val typeDropdown = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.type_dropdown)
        val validityDropdown = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.validity_dropdown)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.amount_input)
        val totalInput = dialogView.findViewById<TextInputEditText>(R.id.total_input)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)

        // Get references to the TextInputLayouts to set hints
        val dateLayout = dialogView.findViewById<TextInputLayout>(R.id.date_layout)
        val personLayout = dialogView.findViewById<TextInputLayout>(R.id.person_layout)
        val typeLayout = dialogView.findViewById<TextInputLayout>(R.id.type_layout)
        val validityLayout = dialogView.findViewById<TextInputLayout>(R.id.validity_layout)
        val amountLayout = dialogView.findViewById<TextInputLayout>(R.id.amount_layout)
        val totalLayout = dialogView.findViewById<TextInputLayout>(R.id.total_layout)

        // Use adapter's naming method to set field labels
        val tempAdapter = DatabaseAdapter().apply {
            updateData(listOf(record))
        }

        // Set field labels using the adapter
        dateLayout.hint = tempAdapter.getDisplayNameForProperty("date")
        personLayout.hint = tempAdapter.getDisplayNameForProperty("person")
        typeLayout.hint = tempAdapter.getDisplayNameForProperty("type")
        validityLayout.hint = tempAdapter.getDisplayNameForProperty("validity")
        amountLayout.hint = tempAdapter.getDisplayNameForProperty("amount")
        totalLayout.hint = tempAdapter.getDisplayNameForProperty("total")

        // Set up type dropdown
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,
            listOf("Alfa", "Touch"))
        typeDropdown.setAdapter(typeAdapter)

        // Set up validity dropdown
        val validityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,
            (1..12).map { "${it}M" })
        validityDropdown.setAdapter(validityAdapter)

        // Ensure clicking the dropdown shows the list
        typeDropdown.setOnClickListener { typeDropdown.showDropDown() }
        validityDropdown.setOnClickListener { validityDropdown.showDropDown() }

        // Apply number formatting to numeric fields
        formatNumberWithCommas(amountInput)
        formatNumberWithCommas(totalInput)

        // Set date picker for date field
        setupDatePicker(dateInput)

        // Populate dialog with the record values
        dateInput.setText(record.date)
        personInput.setText(record.person)
        typeDropdown.setText(record.type, false)
        validityDropdown.setText(record.validity, false)
        amountInput.setText(record.amount.toString())
        totalInput.setText(record.total.toString())

        // Set up button actions
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            if (validateAndSaveVbstIn(record, dateInput, personInput, typeDropdown,
                    validityDropdown, amountInput, totalInput)) {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun validateAndSaveVbstIn(
        record: VBSTIN,
        dateInput: TextInputEditText,
        personInput: TextInputEditText,
        typeDropdown: MaterialAutoCompleteTextView,
        validityDropdown: MaterialAutoCompleteTextView,
        amountInput: TextInputEditText,
        totalInput: TextInputEditText
    ): Boolean {
        // Get values
        val date = dateInput.text.toString()
        val person = personInput.text.toString()
        val type = typeDropdown.text.toString()
        val validity = validityDropdown.text.toString()
        val amountStr = amountInput.text.toString().replace(",", "")
        val totalStr = totalInput.text.toString().replace(",", "")

        // Validate inputs
        if (date.isEmpty() || person.isEmpty() || type.isEmpty() || validity.isEmpty()
            || amountStr.isEmpty() || totalStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            // Parse values
            val amount = amountStr.toDouble()
            val total = totalStr.toDouble()

            // Rate is calculated automatically by the entity
            val updatedRecord = VBSTIN(
                id = record.id,
                date = date,
                person = person,
                type = type,
                validity = validity,
                amount = amount,
                total = total
            )

            // Save to database
            lifecycleScope.launch(Dispatchers.IO) {
                appDao.insertVbstIn(updatedRecord)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Record updated successfully", Toast.LENGTH_SHORT).show()
                    loadTableData() // Refresh data
                }
            }
            return true
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Invalid number format", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun showEditVbstOutDialog(record: VBSTOUT) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_vbstout, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        // Get references to all views
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.date_input)
        val personInput = dialogView.findViewById<TextInputEditText>(R.id.person_input)
        val typeDropdown = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.type_dropdown)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.amount_input)
        val sellrateInput = dialogView.findViewById<TextInputEditText>(R.id.sellrate_input)
        val profitInput = dialogView.findViewById<TextInputEditText>(R.id.profit_input)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)

        // Get references to TextInputLayouts for hints
        val dateLayout = dialogView.findViewById<TextInputLayout>(R.id.date_layout)
        val personLayout = dialogView.findViewById<TextInputLayout>(R.id.person_layout)
        val typeLayout = dialogView.findViewById<TextInputLayout>(R.id.type_layout)
        val amountLayout = dialogView.findViewById<TextInputLayout>(R.id.amount_layout)
        val sellrateLayout = dialogView.findViewById<TextInputLayout>(R.id.sellrate_layout)
        val profitLayout = dialogView.findViewById<TextInputLayout>(R.id.profit_layout)

        // Update dialog title
        dialogView.findViewById<TextView>(R.id.dialog_title).text = "Edit VBSTOUT Record"

        // Use adapter's naming method to set field labels
        val tempAdapter = DatabaseAdapter().apply {
            updateData(listOf(record))
        }

        // Set field labels using the adapter
        dateLayout.hint = tempAdapter.getDisplayNameForProperty("date")
        personLayout.hint = tempAdapter.getDisplayNameForProperty("person")
        typeLayout.hint = tempAdapter.getDisplayNameForProperty("type")
        amountLayout.hint = tempAdapter.getDisplayNameForProperty("amount")
        sellrateLayout.hint = tempAdapter.getDisplayNameForProperty("sellrate")
        profitLayout.hint = tempAdapter.getDisplayNameForProperty("profit")

        // Set up type dropdown
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,
            listOf("Alfa", "Touch"))
        typeDropdown.setAdapter(typeAdapter)

        // Ensure clicking the dropdown shows the list
        typeDropdown.setOnClickListener { typeDropdown.showDropDown() }

        // Apply number formatting to numeric fields
        formatNumberWithCommas(amountInput)
        formatNumberWithCommas(sellrateInput)
        formatNumberWithCommas(profitInput)

        // Set up date picker
        setupDatePicker(dateInput)

        // Function to calculate profit when amount or sellrate changes
        val updateProfitField = {
            try {
                val amount = amountInput.text.toString().replace(",", "").toDoubleOrNull() ?: 0.0
                val sellrate = sellrateInput.text.toString().replace(",", "").toDoubleOrNull() ?: 0.0

                if (amount > 0 && sellrate > 0) {
                    // This is a simplified profit calculation - adjust as needed for your business logic
                    val profit = amount * sellrate * 0.05 // Example: 5% profit margin
                    profitInput.setText(String.format("%.2f", profit))
                }
            } catch (e: Exception) {
                // Ignore calculation errors
            }
        }

        // Set text watchers to update the profit field
        amountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateProfitField() }
        })

        sellrateInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateProfitField() }
        })

        // Populate dialog with the record values
        dateInput.setText(record.date)
        personInput.setText(record.person)
        typeDropdown.setText(record.type, false)
        amountInput.setText(record.amount.toString())
        sellrateInput.setText(record.sellrate.toString())
        profitInput.setText(record.profit.toString())

        // Set up button actions
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            if (validateAndSaveVbstOut(record, dateInput, personInput, typeDropdown,
                    amountInput, sellrateInput, profitInput)) {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun validateAndSaveVbstOut(
        record: VBSTOUT,
        dateInput: TextInputEditText,
        personInput: TextInputEditText,
        typeDropdown: MaterialAutoCompleteTextView,
        amountInput: TextInputEditText,
        sellrateInput: TextInputEditText,
        profitInput: TextInputEditText
    ): Boolean {
        // Get values
        val date = dateInput.text.toString()
        val person = personInput.text.toString()
        val type = typeDropdown.text.toString()
        val amountStr = amountInput.text.toString().replace(",", "")
        val sellrateStr = sellrateInput.text.toString().replace(",", "")
        val profitStr = profitInput.text.toString().replace(",", "")

        // Validate inputs
        if (date.isEmpty() || person.isEmpty() || type.isEmpty() ||
            amountStr.isEmpty() || sellrateStr.isEmpty() || profitStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            // Parse values
            val amount = amountStr.toDouble()
            val sellrate = sellrateStr.toDouble()
            val profit = profitStr.toDouble()

            // Create updated record
            val updatedRecord = VBSTOUT(
                id = record.id,
                date = date,
                person = person,
                amount = amount,
                sellrate = sellrate,
                type = type,
                profit = profit
            )

            // Save to database
            lifecycleScope.launch(Dispatchers.IO) {
                appDao.insertVbstOut(updatedRecord)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Record updated successfully", Toast.LENGTH_SHORT).show()
                    loadTableData() // Refresh data
                }
            }
            return true
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Invalid number format", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    // Placeholder method for USDT editing
    private fun showEditUsdtDialog(record: USDT) {
        // Implement similar to the other edit dialogs
        Toast.makeText(requireContext(), "USDT editing to be implemented", Toast.LENGTH_SHORT).show()
    }

    private fun setupDatePicker(dateInput: TextInputEditText) {
        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()

            // Try to parse existing date
            try {
                val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                dateFormat.parse(dateInput.text.toString())?.let {
                    calendar.time = it
                }
            } catch (e: Exception) {
                // Use current date if parsing fails
            }

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, dayOfMonth)
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    dateInput.setText(dateFormat.format(selectedDate.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun loadTableData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val records = when (currentTable) {
                "DBT" -> appDao.getAllIncome()
                "DST" -> appDao.getAllExpense()
                "VBSTIN" -> appDao.getAllVbstIn()
                "VBSTOUT" -> appDao.getAllVbstOut()
                "USDT" -> {
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
                // First update the adapter with the data to set the entity type
                adapter.updateData(records)

                // Now that the entity type is set, update column headers
                updateColumnHeaders(records)

                // Apply search filter if active
                if (!searchQuery.isNullOrEmpty() && !searchColumn.isNullOrEmpty() && records.isNotEmpty()) {
                    performSearch(searchColumn!!, searchQuery!!)
                }
            }
        }
    }

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
                    (allRecords as List<*>).filterIsInstance<DBT>().filter { record ->
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
                    (allRecords as List<*>).filterIsInstance<DST>().filter { record ->
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
                    (allRecords as List<*>).filterIsInstance<VBSTIN>().filter { record ->
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
                    (allRecords as List<*>).filterIsInstance<VBSTOUT>().filter { record ->
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
                    (allRecords as List<*>).filterIsInstance<USDT>().filter { record ->
                        when (column) {
                            "id" -> record.id.toString() == searchTerm
                            "date" -> record.date.contains(searchTerm, ignoreCase = true)
                            "person" -> record.person.contains(searchTerm, ignoreCase = true)
                            "amountUsdt" -> record.amountUsdt.toString().contains(searchTerm)
                            "amountCash" -> record.amountCash.toString().contains(searchTerm)
                            "type" -> record.type.contains(searchTerm, ignoreCase = true)
                            else -> false
                        }
                    }
                }
                else -> emptyList<Any>()
            }

            withContext(Dispatchers.Main) {
                adapter.updateData(results)
                val message = if (results.isEmpty()) "No results found" else "${results.size} results found"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearSearch() {
        if (searchQuery != null && searchColumn != null) {
            searchQuery = null
            searchColumn = null
            adapter.updateData(allRecords)
            (activity as? AppCompatActivity)?.supportActionBar?.title = "Databases"
            Toast.makeText(requireContext(), "Search cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(record: Any) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Delete Record")
        builder.setMessage("Are you sure you want to delete this record?")
        builder.setPositiveButton("Delete") { dialog, _ ->
            deleteRecord(record)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun deleteRecord(record: Any) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                when (record) {
                    is DBT -> {
                        appDao.deleteIncome(record.id)
                        val remainingRecords = appDao.getAllIncome()
                        if (remainingRecords.isEmpty()) appDao.resetDbtSequence()
                    }
                    is DST -> {
                        appDao.deleteExpense(record.id)
                        val remainingRecords = appDao.getAllExpense()
                        if (remainingRecords.isEmpty()) appDao.resetDstSequence()
                    }
                    is VBSTIN -> {
                        appDao.deleteVbstIn(record.id)
                        val remainingRecords = appDao.getAllVbstIn()
                        if (remainingRecords.isEmpty()) appDao.resetVbstInSequence()
                    }
                    is VBSTOUT -> {
                        appDao.deleteVbstOut(record.id)
                        val remainingRecords = appDao.getAllVbstOut()
                        if (remainingRecords.isEmpty()) appDao.resetVbstOutSequence()
                    }
                    is USDT -> {
                        appDao.deleteUsdt(record.id)
                        val remainingRecords = appDao.getAllUsdt()
                        if (remainingRecords.isEmpty()) appDao.resetUsdtSequence()
                    }
                    else -> throw IllegalArgumentException("Unsupported record type")
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Record deleted", Toast.LENGTH_SHORT).show()
                    loadTableData()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Error deleting record: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}