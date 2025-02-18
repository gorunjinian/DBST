package com.gorunjinian.dbst

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

class ValidityFragment : Fragment() {

    private lateinit var dateInput: TextInputEditText
    private lateinit var personInput: TextInputEditText
    private lateinit var typeDropdown: MaterialAutoCompleteTextView
    private lateinit var validityDropdown: MaterialAutoCompleteTextView
    private lateinit var amountInput: TextInputEditText
    private lateinit var RateInput: TextInputEditText
    private lateinit var creditInButton: MaterialButton
    private lateinit var creditOutButton: MaterialButton
    private lateinit var validityLayout: TextInputLayout
    private lateinit var sellRateLayout: TextInputLayout

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
        RateInput = view.findViewById(R.id.rate_input)
        creditInButton = view.findViewById(R.id.credit_in_button)
        creditOutButton = view.findViewById(R.id.credit_out_button)
        validityLayout = view.findViewById(R.id.validity_layout)
        sellRateLayout = view.findViewById(R.id.sell_rate_layout)

        // Automatically Set Today's Date
        setTodayDate()

        // Set default values
        RateInput.setText("0")

        // Set up Date Picker
        dateInput.setOnClickListener {
            showDatePicker()
        }

        // Set up default dropdowns
        setupDropdown(typeDropdown, listOf("Alfa", "Touch"))
        setupDropdown(validityDropdown, (1..12).map { "${it}M" })

        // Handle button click logic
        creditInButton.setOnClickListener { selectCreditType(true) }
        creditOutButton.setOnClickListener { selectCreditType(false) }

        // Set Credit Out as Default Selection
        selectCreditType(false)
    }
    private fun setTodayDate() {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        dateInput.setText(dateFormat.format(today.time))
    }

    private fun setupDropdown(dropdown: MaterialAutoCompleteTextView, options: List<String>) {
        dropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options))
    }

    private fun selectCreditType(isCreditIn: Boolean) {
        resetButtonStyles()

        if (isCreditIn) {
            creditInButton.strokeWidth = 10
            creditInButton.strokeColor =
                ColorStateList.valueOf(MaterialColors.getColor(creditInButton, com.google.android.material.R.attr.colorOnBackground))


            validityLayout.visibility = View.VISIBLE
            sellRateLayout.visibility = View.GONE
        } else {
            creditOutButton.strokeWidth = 10
            creditOutButton.strokeColor =
                ColorStateList.valueOf(MaterialColors.getColor(creditOutButton, com.google.android.material.R.attr.colorOnBackground))


            validityLayout.visibility = View.GONE
            sellRateLayout.visibility = View.VISIBLE
        }
    }

    private fun resetButtonStyles() {
        creditInButton.strokeWidth = 0
        creditOutButton.strokeWidth = 0
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

}
