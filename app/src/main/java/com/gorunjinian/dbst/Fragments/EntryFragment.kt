package com.gorunjinian.dbst.fragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
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

@SuppressLint("SetTextI18n")
class EntryFragment : Fragment() {

    //inputs
    private lateinit var dateInput: TextInputEditText
    private lateinit var personInput: TextInputEditText
    private lateinit var amountInput: TextInputEditText
    private lateinit var amountExchangedInput: TextInputEditText
    private lateinit var rateInput: TextInputEditText
    private lateinit var typeDropdown: MaterialAutoCompleteTextView

    //layouts
    private lateinit var amountExchangedLayout: TextInputLayout
    private lateinit var amountLayout: TextInputLayout

    //buttons
    private lateinit var incomeButton: MaterialButton
    private lateinit var expenseButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var clearButton: MaterialButton
    private lateinit var undoButton: MaterialButton
    private var undoCountDownTimer: CountDownTimer? = null


    //database components
    private lateinit var database: AppDatabase
    private lateinit var appDao: AppDao
    private var lastEntryTime: Long = 0L
    private var lastEntryType: String? = null  // "income" or "expense"
    private var lastEntry: Any? = null         // holds the last inserted DBT or DST instance


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Enable menu options for this fragment
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_entry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Views
        dateInput = view.findViewById(R.id.date_input)
        personInput = view.findViewById(R.id.person_input)
        typeDropdown = view.findViewById(R.id.type_dropdown)
        rateInput = view.findViewById(R.id.rate_input)
        amountExchangedLayout = view.findViewById(R.id.amount_exchanged_layout)
        amountExchangedInput = view.findViewById(R.id.amount_exchanged_input)
        amountLayout = view.findViewById(R.id.amount_expensed_layout)
        amountInput = view.findViewById(R.id.amount_expensed_input)
        incomeButton = view.findViewById(R.id.income_button)
        expenseButton = view.findViewById(R.id.expense_button)
        saveButton = view.findViewById(R.id.save_button)
        clearButton = view.findViewById(R.id.clear_button)
        undoButton = view.findViewById(R.id.undo_button)

        undoButton.setOnClickListener { undoButtonAction() }

        // Disable undo button by default
        undoButton.isEnabled = false

        setupFocusHandling()


        // Initialize Room Database
        database = AppDatabase.getDatabase(requireContext())
        appDao = database.appDao()

        // Restore previously selected button (Income/Expense)
        val prefs = requireActivity().getSharedPreferences("app_prefs", 0)
        val isExpenseSelected = prefs.getBoolean("is_expense_selected", true)
        val lastSelectedType =
            prefs.getString(if (isExpenseSelected) "expense_type" else "income_type", "")


        // Set up default dropdown options based on selection
        setupTypeDropdown(
            if (isExpenseSelected) getExpenseTypes() else getIncomeTypes(),
            lastSelectedType
        )

        formatNumberWithCommas(amountInput)
        formatNumberWithCommas(amountExchangedInput)
        formatNumberWithCommas(rateInput)

        // Ensure clicking the field shows the dropdown
        typeDropdown.setOnClickListener { typeDropdown.showDropDown() }

        // Restore button selection
        selectButton(if (isExpenseSelected) expenseButton else incomeButton, isExpenseSelected)

        // Set up Date Picker
        dateInput.setOnClickListener { showDatePicker() }

        // Set today's date
        setTodayDate()

        // Setup auto-open for dropdowns
        setupAutoOpenDropdowns()

        clearButton.setOnClickListener { clearInputFields() }

        // Set up Button Click Listeners
        incomeButton.setOnClickListener { selectButton(it as MaterialButton, isExpense = false) }
        expenseButton.setOnClickListener { selectButton(it as MaterialButton, isExpense = true) }

        // Save Button Logic
        saveButton.setOnClickListener {
            if (expenseButton.strokeWidth > 0) {
                saveExpense()
            } else {
                saveIncome()
            }
        }

    }

    private fun getIncomeTypes(): List<String> = listOf(
        "Income", "Buy", "Return", "Profit", "Validity",
        "Loan", "Gift", "Other", "N/A")

    private fun getExpenseTypes(): List<String> = listOf(
        "FOOD", "GROCERIES", "EXCHANGE","WHISH TOPUP", "WELLBEING",
        "BANK TOPUP", "TECH", "DEBT", "OTHER")

    private fun resetButtonStyles() {
        incomeButton.strokeWidth = 0
        expenseButton.strokeWidth = 0
    }

    private fun saveIncome() {
        // Validate inputs
        val date = dateInput.text.toString()
        val person = personInput.text.toString()
        val amountText = amountInput.text.toString()
        val rateText = rateInput.text.toString()
        val type = typeDropdown.text.toString()

        if (date.isEmpty() || person.isEmpty() || amountText.isEmpty() || rateText.isEmpty() || type.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.replace(",", "").toDouble()
        val rate = rateText.replace(",", "").toDoubleOrNull() ?: 1.0 // Default rate to 1.0 if null
        val totalLBP = amount * rate

        lifecycleScope.launch {
            val incomeEntry = DBT(
                date = date,
                person = person,
                amount = amount,
                rate = rate,
                type = type,
                totalLBP = totalLBP
            )
            // Capture the generated id
            val newId = appDao.insertIncome(incomeEntry)
            // Update the entry with the generated id
            val savedIncomeEntry = incomeEntry.copy(id = newId.toInt())
            Toast.makeText(requireContext(), "Income Entry Saved!", Toast.LENGTH_SHORT).show()
            clearInputFields()

            // Track the last inserted entry for undo
            lastEntryTime = System.currentTimeMillis()
            lastEntryType = "income"
            lastEntry = savedIncomeEntry

            // Start the 15 second countdown
            startUndoCountdown()
        }


    }

    private fun saveExpense() {
        // Validate inputs
        val date = dateInput.text.toString()
        val person = personInput.text.toString()
        val amountText = amountInput.text.toString()
        val rateText = rateInput.text.toString()
        val type = typeDropdown.text.toString()
        val amountExchangedText = amountExchangedInput.text.toString()

        if (date.isEmpty() || person.isEmpty() || amountText.isEmpty() || rateText.isEmpty() || type.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amountExpensed = amountText.replace(",", "").toDouble()
        val rate = rateText.replace(",", "").toDoubleOrNull() ?: 1.0 // Default rate to 1.0 if null
        val amountExchanged = if (amountExchangedText.isNotEmpty()) amountExchangedText.replace(",", "").toDouble() else 0.0
        val exchangedLBP = amountExchanged * rate // Correctly calculate exchangedLBP

        lifecycleScope.launch {
            val expenseEntry = DST(
                date = date,
                person = person,
                amountExpensed = amountExpensed,
                amountExchanged = amountExchanged,
                rate = rate,
                type = type,
                exchangedLBP = exchangedLBP
            )
            val newId = appDao.insertExpense(expenseEntry)
            val savedExpenseEntry = expenseEntry.copy(id = newId.toInt())
            Toast.makeText(requireContext(), "Expense Entry Saved!", Toast.LENGTH_SHORT).show()
            clearInputFields()

            // Track the last inserted entry for undo
            lastEntryTime = System.currentTimeMillis()
            lastEntryType = "expense"
            lastEntry = savedExpenseEntry

            // Start the 15 second countdown
            startUndoCountdown()
        }


    }

    private fun undoButtonAction() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEntryTime <= 30000) {  // within 30 seconds
            // Cancel the countdown so it stops updating the button text
            undoCountDownTimer?.cancel()
            // Reset button text immediately and disable it
            undoButton.text = "Undo"
            undoButton.isEnabled = false

            lifecycleScope.launch {
                when (lastEntryType) {
                    "income" -> {
                        lastEntry?.let { entry ->
                            val incomeEntry = entry as DBT
                            appDao.deleteIncome(incomeEntry.id)
                            // Switch UI to Income mode and repopulate fields
                            withContext(Dispatchers.Main) {
                                selectButton(incomeButton, isExpense = false)
                                dateInput.setText(incomeEntry.date)
                                personInput.setText(incomeEntry.person)
                                amountInput.setText(incomeEntry.amount.toString())
                                rateInput.setText(incomeEntry.rate.toString())
                                typeDropdown.setText(incomeEntry.type, false)
                            }
                        }
                    }
                    "expense" -> {
                        lastEntry?.let { entry ->
                            val expenseEntry = entry as DST
                            appDao.deleteExpense(expenseEntry.id)
                            // Switch UI to Expense mode and repopulate fields
                            withContext(Dispatchers.Main) {
                                selectButton(expenseButton, isExpense = true)
                                dateInput.setText(expenseEntry.date)
                                personInput.setText(expenseEntry.person)
                                amountInput.setText(expenseEntry.amountExpensed.toString())
                                rateInput.setText(expenseEntry.rate.toString())
                                typeDropdown.setText(expenseEntry.type, false)
                                amountExchangedInput.setText(expenseEntry.amountExchanged.toString())
                            }
                        }
                    }
                }
                // Clear the tracking variables after undoing
                lastEntryTime = 0L
                lastEntryType = null
                lastEntry = null
            }
            Toast.makeText(requireContext(), "Last entry undone", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Undo period expired", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startUndoCountdown() {
        // Cancel any previous countdown
        undoCountDownTimer?.cancel()

        // Enable the undo button and set initial text
        undoButton.isEnabled = true
        undoButton.text = "Undo (30s)"

        // Create and start a new CountDownTimer
        undoCountDownTimer = object : CountDownTimer(30000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                // Update the button text with remaining seconds
                undoButton.text = "Undo (${secondsRemaining}s)"
            }

            @SuppressLint("SetTextI18n")
            override fun onFinish() {
                // Once finished, reset the button text and disable it
                undoButton.text = "Undo"
                undoButton.isEnabled = false
                // Optionally, reset tracking for last entry if needed
                lastEntryTime = 0L
                lastEntryType = null
                lastEntry = null
            }
        }.start()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
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
        )
        datePickerDialog.show()
    }

    private fun clearInputFields() {
        personInput.text?.clear()
        amountInput.text?.clear()
        amountExchangedInput.text?.clear()
        rateInput.text?.clear()
        typeDropdown.text?.clear()
    }

    private fun setupTypeDropdown(types: List<String>, selectedValue: String?) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            types
        )
        typeDropdown.setAdapter(adapter)

        if (!selectedValue.isNullOrEmpty()) {
            typeDropdown.setText(selectedValue, false)
        }
    }

    private fun selectButton(selectedButton: MaterialButton, isExpense: Boolean) {
        // Reset button styles
        resetButtonStyles()

        // Highlight the selected button
        selectedButton.strokeWidth = 12
        selectedButton.strokeColor =
            ColorStateList.valueOf(
                MaterialColors.getColor(
                    selectedButton,
                    com.google.android.material.R.attr.colorOnBackground
                )
            )

        // Save selection in SharedPreferences
        val prefs = requireActivity().getSharedPreferences("app_prefs", 0)
        prefs.edit()
            .putBoolean("is_expense_selected", isExpense)
            .apply()

        // Restore last selected type (if exists)
        val lastSelectedType = prefs.getString(if (isExpense) "expense_type" else "income_type", "")

        // Update UI based on selection
        if (isExpense) {
            amountLayout.hint = "Amount Expensed"
            amountExchangedLayout.visibility = View.VISIBLE
            amountExchangedInput.setText("0") // Reset Amount Exchanged to default
            setupTypeDropdown(getExpenseTypes(), lastSelectedType) // Load expense categories
        } else {
            amountLayout.hint = "Amount"
            rateInput.setText("0") // Rate field to default 0 for Income
            amountExchangedLayout.visibility = View.GONE
            setupTypeDropdown(getIncomeTypes(), lastSelectedType) // Load income categories
        }
    }

    // Add this function to EntryFragment class
    private fun setupFocusHandling() {
        // Fix focus issues when moving from person input to amount input
        personInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                // Explicitly set focus to amount field with a slight delay to avoid race conditions
                amountInput.postDelayed({
                    amountInput.requestFocus()
                    // Select all text in the field
                    amountInput.selectAll()
                }, 50)
                true
            } else {
                false
            }
        }

        // Also add a focus listener to ensure text is selected when field gets focus
        amountInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                amountInput.postDelayed({
                    amountInput.selectAll()
                }, 50)
            }
        }
    }

    private fun setupAutoOpenDropdowns() {
        // Handle keyboard to dropdown transition
        rateInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                // Hide keyboard
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(rateInput.windowToken, 0)

                // Post a delay to show dropdown after keyboard is hidden
                typeDropdown.postDelayed({
                    typeDropdown.requestFocus()
                    typeDropdown.showDropDown()
                }, 200)

                true // consume the action
            } else {
                false // don't consume the action
            }
        }
    }

    private fun setTodayDate() {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        dateInput.setText(dateFormat.format(today.time))
    }
}