package com.gorunjinian.dbst.fragments

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
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
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.*

class EntryFragment : Fragment() {

    private lateinit var dateInput: TextInputEditText
    private lateinit var personInput: TextInputEditText
    private lateinit var amountInput: TextInputEditText
    private lateinit var amountExchangedInput: TextInputEditText
    private lateinit var rateInput: TextInputEditText
    private lateinit var typeDropdown: MaterialAutoCompleteTextView
    private lateinit var amountExchangedLayout: TextInputLayout
    private lateinit var amountLayout: TextInputLayout
    private lateinit var incomeButton: MaterialButton
    private lateinit var expenseButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var clearButton: MaterialButton

    private lateinit var database: EntryDatabase
    private lateinit var entryDao: EntryDao

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
        amountInput = view.findViewById(R.id.amount_input)
        typeDropdown = view.findViewById(R.id.type_dropdown)
        rateInput = view.findViewById(R.id.rate_input)
        amountExchangedLayout = view.findViewById(R.id.amount_exchanged_layout)
        amountExchangedInput = view.findViewById(R.id.amount_exchanged_input)
        amountLayout = view.findViewById(R.id.amount_layout)
        incomeButton = view.findViewById(R.id.income_button)
        expenseButton = view.findViewById(R.id.expense_button)
        saveButton = view.findViewById(R.id.save_button)
        clearButton = view.findViewById(R.id.clear_button)

        // Initialize Room Database
        database = EntryDatabase.getDatabase(requireContext())
        entryDao = database.entryDao()

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

        clearButton.setOnClickListener {
            clearInputFields()
        }

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
        "FOOD", "GROCERIES", "EXCHANGE", "WELLBEING",
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
            entryDao.insertIncome(incomeEntry)
            Toast.makeText(requireContext(), "Income Entry Saved!", Toast.LENGTH_SHORT).show()
            clearInputFields()
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
        val exchangedLBP = amountExchanged * rate // ✅ Correctly calculate exchangedLBP

        lifecycleScope.launch {
            val expenseEntry = DST(
                date = date,
                person = person,
                amountExpensed = amountExpensed,
                amountExchanged = amountExchanged,
                rate = rate,
                type = type,
                exchangedLBP = exchangedLBP // ✅ Store the correct exchangedLBP
            )
            entryDao.insertExpense(expenseEntry)
            Toast.makeText(requireContext(), "Expense Entry Saved!", Toast.LENGTH_SHORT).show()
            clearInputFields()
        }
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
        selectedButton.strokeWidth = 10
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
            amountExchangedLayout.visibility = View.GONE
            setupTypeDropdown(getIncomeTypes(), lastSelectedType) // Load income categories
        }
    }

    private fun setTodayDate() {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        dateInput.setText(dateFormat.format(today.time))
    }
}
