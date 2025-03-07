package com.gorunjinian.dbst.fragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
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
import java.util.Calendar
import java.util.Locale

@SuppressLint("SetTextI18n")
class EntryFragment : Fragment() {

    // Inputs
    private lateinit var dateInput: TextInputEditText
    private lateinit var personInput: TextInputEditText
    private lateinit var amountInput: TextInputEditText
    private lateinit var amountExchangedInput: TextInputEditText
    private lateinit var rateInput: TextInputEditText
    private lateinit var typeDropdown: MaterialAutoCompleteTextView

    // Layouts
    private lateinit var amountExchangedLayout: TextInputLayout
    private lateinit var amountLayout: TextInputLayout

    // Buttons
    private lateinit var incomeButton: MaterialButton
    private lateinit var expenseButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var clearButton: MaterialButton
    private lateinit var undoButton: MaterialButton
    private var undoCountDownTimer: CountDownTimer? = null

    // DB
    private lateinit var database: AppDatabase
    private lateinit var appDao: AppDao

    // Undo tracking
    private var lastEntryTime: Long = 0L
    private var lastEntryType: String? = null
    private var lastEntry: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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

        // DB
        database = AppDatabase.getDatabase(requireContext())
        appDao = database.appDao()

        undoButton.setOnClickListener { undoButtonAction() }
        undoButton.isEnabled = false

        setupFocusHandling()

        // Setup toggle group
        val toggleGroup: MaterialButtonToggleGroup = view.findViewById(R.id.toggle_group)
        // Force expense by default
        toggleGroup.check(R.id.expense_button)
        // Listen for toggles
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.income_button -> selectButton(isExpense = false)
                    R.id.expense_button -> selectButton(isExpense = true)
                }
            }
        }
        // Manually set up the initial UI for expense
        selectButton(isExpense = true)

        // Comma formatting
        formatNumberWithCommas(amountInput)
        formatNumberWithCommas(amountExchangedInput)
        formatNumberWithCommas(rateInput)

        // Show dropdown on click
        typeDropdown.setOnClickListener { typeDropdown.showDropDown() }

        // Date
        dateInput.setOnClickListener { showDatePicker() }
        setTodayDate()

        // Next fields
        setupAutoOpenDropdowns()

        clearButton.setOnClickListener { clearInputFields() }

        // Save
        saveButton.setOnClickListener {
            if (expenseButton.isChecked) saveExpense() else saveIncome()
        }
    }

    // List of Income types
    private fun getIncomeTypes(): List<String> = listOf(
        "Income", "Buy", "Return", "Profit",
        "Validity", "Loan", "Gift", "Other", "N/A"
    )

    // List of Expense types
    private fun getExpenseTypes(): List<String> = listOf(
        "FOOD", "GROCERIES", "EXCHANGE", "WHISH TOPUP",
        "WELLBEING", "BANK TOPUP", "TECH", "DEBT", "OTHER"
    )

    /**
     * Switching layout to Income or Expense
     * We ensure the 'type' field is automatically cleared here.
     */
    private fun selectButton(isExpense: Boolean) {
        // Clear whatever was selected in the type dropdown
        typeDropdown.setText("", false)

        if (isExpense) {
            amountLayout.hint = "Amount Expensed"
            amountExchangedLayout.visibility = View.VISIBLE
            amountExchangedInput.setText("0")

            setupTypeDropdown(getExpenseTypes())
        } else {
            amountLayout.hint = "Amount"
            amountExchangedLayout.visibility = View.GONE

            // For example, reset the rate if you like
            rateInput.setText("0")

            setupTypeDropdown(getIncomeTypes())
        }
    }

    /**
     * Setup the adapter for the type dropdown.
     * Notice we only pass the list, no "previous" type since we want it empty now.
     */
    private fun setupTypeDropdown(types: List<String>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            types
        )
        typeDropdown.setAdapter(adapter)
    }

    private fun saveIncome() {
        val date = dateInput.text.toString()
        val person = personInput.text.toString()
        val amountText = amountInput.text.toString()
        val rateText = rateInput.text.toString()
        val type = typeDropdown.text.toString()

        if (date.isEmpty() || person.isEmpty() || amountText.isEmpty() ||
            rateText.isEmpty() || type.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.replace(",", "").toDouble()
        val rate = rateText.replace(",", "").toDoubleOrNull() ?: 1.0
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
            val newId = appDao.insertIncome(incomeEntry)
            val savedIncomeEntry = incomeEntry.copy(id = newId.toInt())

            Toast.makeText(requireContext(), "Income Entry Saved!", Toast.LENGTH_SHORT).show()
            clearInputFields()

            // For "undo"
            lastEntryTime = System.currentTimeMillis()
            lastEntryType = "income"
            lastEntry = savedIncomeEntry
            startUndoCountdown()
        }
    }

    private fun saveExpense() {
        val date = dateInput.text.toString()
        val person = personInput.text.toString()
        val amountText = amountInput.text.toString()
        val rateText = rateInput.text.toString()
        val type = typeDropdown.text.toString()
        val amountExchangedText = amountExchangedInput.text.toString()

        if (date.isEmpty() || person.isEmpty() || amountText.isEmpty() ||
            rateText.isEmpty() || type.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amountExpensed = amountText.replace(",", "").toDouble()
        val rate = rateText.replace(",", "").toDoubleOrNull() ?: 1.0
        val amountExchanged = if (amountExchangedText.isNotEmpty()) {
            amountExchangedText.replace(",", "").toDouble()
        } else 0.0
        val exchangedLBP = amountExchanged * rate

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

            // For "undo"
            lastEntryTime = System.currentTimeMillis()
            lastEntryType = "expense"
            lastEntry = savedExpenseEntry
            startUndoCountdown()
        }
    }

    private fun undoButtonAction() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEntryTime <= 30000) {
            undoCountDownTimer?.cancel()
            undoButton.text = "Undo"
            undoButton.isEnabled = false

            lifecycleScope.launch {
                when (lastEntryType) {
                    "income" -> {
                        (lastEntry as? DBT)?.let { incomeEntry ->
                            appDao.deleteIncome(incomeEntry.id)
                            withContext(Dispatchers.Main) {
                                selectButton(isExpense = false)
                                dateInput.setText(incomeEntry.date)
                                personInput.setText(incomeEntry.person)
                                amountInput.setText(incomeEntry.amount.toString())
                                rateInput.setText(incomeEntry.rate.toString())
                                typeDropdown.setText(incomeEntry.type, false)
                            }
                        }
                    }
                    "expense" -> {
                        (lastEntry as? DST)?.let { expenseEntry ->
                            appDao.deleteExpense(expenseEntry.id)
                            withContext(Dispatchers.Main) {
                                selectButton(isExpense = true)
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
        undoCountDownTimer?.cancel()
        undoButton.isEnabled = true
        undoButton.text = "Undo (30s)"

        undoCountDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                undoButton.text = "Undo (${secondsRemaining}s)"
            }
            override fun onFinish() {
                undoButton.text = "Undo"
                undoButton.isEnabled = false
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

    private fun setupFocusHandling() {
        personInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                amountInput.postDelayed({
                    amountInput.requestFocus()
                    amountInput.selectAll()
                }, 50)
                true
            } else {
                false
            }
        }

        amountInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                amountInput.postDelayed({
                    amountInput.selectAll()
                }, 50)
            }
        }
    }

    private fun setupAutoOpenDropdowns() {
        rateInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(rateInput.windowToken, 0)

                typeDropdown.postDelayed({
                    typeDropdown.requestFocus()
                    typeDropdown.showDropDown()
                }, 100)
                true
            } else {
                false
            }
        }
    }

    private fun setTodayDate() {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        dateInput.setText(dateFormat.format(today.time))
    }
}
