package com.gorunjinian.dbst.fragments

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.gorunjinian.dbst.MyApplication.Companion.formatNumberWithCommas
import com.gorunjinian.dbst.R

import java.text.SimpleDateFormat
import java.util.*

class TetherFragment : Fragment() {

    private lateinit var dateInput: TextInputEditText
    private lateinit var personInput: TextInputEditText
    private lateinit var usdtAmountInput: TextInputEditText
    private lateinit var cashInput: TextInputEditText
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var buyButton: MaterialButton
    private lateinit var sellButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var clearButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_tether, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Views
        dateInput = view.findViewById(R.id.date_input)
        personInput = view.findViewById(R.id.person_input)
        usdtAmountInput = view.findViewById(R.id.usdt_amount_input)
        cashInput = view.findViewById(R.id.cash_input)
        toggleGroup = view.findViewById(R.id.toggle_group)
        buyButton = view.findViewById(R.id.buy_button)
        sellButton = view.findViewById(R.id.sell_button)
        saveButton = view.findViewById(R.id.save_button)
        clearButton = view.findViewById(R.id.clear_button)

        formatNumberWithCommas(cashInput)
        formatNumberWithCommas(usdtAmountInput)

        // Automatically Set Today's Date
        setTodayDate()

        // Improve keyboard navigation between fields
        setupKeyboardNavigation()

        // Automatically select SELL button
        sellButton.isChecked = true

        // Set up Date Picker
        dateInput.setOnClickListener {
            showDatePicker()
        }

        // Save Button Logic
        saveButton.setOnClickListener {
            saveData()
        }

        clearButton.setOnClickListener {
            clearInputFields()
        }
    }

    private fun setTodayDate() {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        dateInput.setText(dateFormat.format(today.time))
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

    private fun saveData() {
        val date = dateInput.text.toString()
        val person = personInput.text.toString()
        val usdtAmount = usdtAmountInput.text.toString()
        val cash = cashInput.text.toString()
        val selectedType = when (toggleGroup.checkedButtonId) {
            R.id.buy_button -> "BUY"
            R.id.sell_button -> "SELL"
            else -> ""
        }

        if (date.isEmpty() || person.isEmpty() || usdtAmount.isEmpty() || cash.isEmpty() || selectedType.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(), "Data Saved:\nType: $selectedType", Toast.LENGTH_LONG).show()
    }

    private fun setupKeyboardNavigation() {
        // Set proper IME options for sequential input
        dateInput.imeOptions = EditorInfo.IME_ACTION_NEXT
        personInput.imeOptions = EditorInfo.IME_ACTION_NEXT
        usdtAmountInput.imeOptions = EditorInfo.IME_ACTION_NEXT
        cashInput.imeOptions = EditorInfo.IME_ACTION_DONE

        // Implement navigation between fields
        personInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                usdtAmountInput.requestFocus()
                true
            } else false
        }

        usdtAmountInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                cashInput.requestFocus()
                true
            } else false
        }

        // When done on cash input, hide keyboard and focus save button
        cashInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(cashInput.windowToken, 0)
                saveButton.requestFocus()
                true
            } else false
        }
    }

    private fun clearInputFields() {
        personInput.text?.clear()
        usdtAmountInput.text?.clear()
        cashInput.text?.clear()
    }
}
