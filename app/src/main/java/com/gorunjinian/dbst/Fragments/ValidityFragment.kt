package com.gorunjinian.dbst.fragments

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*
import android.text.Editable
import android.text.TextWatcher
import com.gorunjinian.dbst.MyApplication.Companion.formatNumberWithCommas
import com.gorunjinian.dbst.R
import java.text.NumberFormat
import java.util.Locale

class ValidityFragment : Fragment() {

    private lateinit var dateInput: TextInputEditText
    private lateinit var personInput: TextInputEditText
    private lateinit var typeDropdown: MaterialAutoCompleteTextView
    private lateinit var validityDropdown: MaterialAutoCompleteTextView
    private lateinit var amountInput: TextInputEditText
    private lateinit var totalInput: TextInputEditText
    private lateinit var rateInput: TextInputEditText
    private lateinit var creditInButton: MaterialButton
    private lateinit var creditOutButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var validityLayout: TextInputLayout
    private lateinit var totalLayout: TextInputLayout
    private lateinit var rateLayout: TextInputLayout
    private lateinit var clearButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_validity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Views
        dateInput = view.findViewById(R.id.date_input)
        personInput = view.findViewById(R.id.person_input)
        typeDropdown = view.findViewById(R.id.type_dropdown)
        validityDropdown = view.findViewById(R.id.validity_dropdown)
        amountInput = view.findViewById(R.id.amount_input)
        totalInput = view.findViewById(R.id.total_input)
        rateInput = view.findViewById(R.id.rate_input)
        creditInButton = view.findViewById(R.id.credit_in_button)
        creditOutButton = view.findViewById(R.id.credit_out_button)
        saveButton = view.findViewById(R.id.save_button)
        validityLayout = view.findViewById(R.id.validity_layout)
        totalLayout = view.findViewById(R.id.total_layout)
        rateLayout = view.findViewById(R.id.rate_layout)
        clearButton = view.findViewById(R.id.clear_button)

        formatNumberWithCommas(amountInput)
        formatNumberWithCommas(totalInput)
        formatNumberWithCommas(rateInput)

        // Automatically Set Today's Date
        setTodayDate()

        // Set default values
        rateInput.setText("0")
        totalInput.setText("0")

        // Set up Date Picker
        dateInput.setOnClickListener {
            showDatePicker()
        }

        // Set up default dropdown options
        setupDropdown(typeDropdown, listOf("Alfa", "Touch"))
        setupDropdown(validityDropdown, (1..12).map { "${it}M" })

        // Ensure clicking dropdown shows the list
        typeDropdown.setOnClickListener { typeDropdown.showDropDown() }
        validityDropdown.setOnClickListener { validityDropdown.showDropDown() }

        //clear button function call
        clearButton.setOnClickListener {
            clearInputFields()
        }

        // Set up button click listeners
        creditInButton.setOnClickListener { selectCreditType(isCreditIn = true) }
        creditOutButton.setOnClickListener { selectCreditType(isCreditIn = false) }

        // Set Credit OUT as Default Selection
        selectCreditType(isCreditIn = false)

        // Save Button Logic
        saveButton.setOnClickListener {
            saveData()
        }
    }

    override fun onResume() {
        super.onResume()
        setupDropdown(typeDropdown, listOf("Alfa", "Touch"))
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

    private fun selectCreditType(isCreditIn: Boolean) {
        resetButtonStyles()

        if (isCreditIn) {
            // Correctly show Credit IN fields
            creditInButton.strokeWidth = 10
            creditInButton.strokeColor =
                ColorStateList.valueOf(MaterialColors.getColor(creditInButton, com.google.android.material.R.attr.colorOnBackground))

            validityLayout.visibility = View.VISIBLE  // Should be visible for Credit OUT
            totalLayout.visibility = View.VISIBLE  // Should be visible for Credit OUT
            rateLayout.visibility = View.GONE  // Should be hidden for Credit OUT
        } else {
            // Correctly show Credit OUT fields
            creditOutButton.strokeWidth = 10
            creditOutButton.strokeColor =
                ColorStateList.valueOf(MaterialColors.getColor(creditOutButton, com.google.android.material.R.attr.colorOnBackground))

            validityLayout.visibility = View.GONE  // Should be hidden for Credit IN
            totalLayout.visibility = View.GONE  // Should be hidden for Credit IN
            rateLayout.visibility = View.VISIBLE  // Should be visible for Credit IN
        }
    }


    private fun resetButtonStyles() {
        creditInButton.strokeWidth = 0
        creditOutButton.strokeWidth = 0
    }

    private fun setupDropdown(dropdown: MaterialAutoCompleteTextView, options: List<String>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            options
        )
        dropdown.setAdapter(adapter)
    }

    private fun saveData() {
        val date = dateInput.text.toString()
        val person = personInput.text.toString()
        val type = typeDropdown.text.toString()
        val amount = amountInput.text.toString()
        val rate = rateInput.text.toString()
        val validity = validityDropdown.text.toString()
        val total = totalInput.text.toString()

        if (date.isEmpty() || person.isEmpty() || type.isEmpty() || amount.isEmpty() ||
            (creditOutButton.strokeWidth > 0 && validity.isEmpty()) ||
            (creditInButton.strokeWidth > 0 && rate.isEmpty())
        ) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(
            requireContext(),
            "Data Saved:\nDate: $date\nPerson: $person\nType: $type\nAmount: $amount\n" +
                    if (creditInButton.strokeWidth > 0) "Rate: $rate" else "Validity: $validity\nTotal: $total",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun clearInputFields() {
        personInput.text?.clear()
        amountInput.text?.clear()
        rateInput.text?.clear()
        typeDropdown.text?.clear()
        totalInput.text?.clear()
        validityDropdown.text?.clear()
    }

}