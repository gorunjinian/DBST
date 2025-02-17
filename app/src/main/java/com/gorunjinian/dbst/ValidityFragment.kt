package com.gorunjinian.dbst

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
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
    private lateinit var sellRateInput: TextInputEditText
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
        sellRateInput = view.findViewById(R.id.sell_rate_input)
        creditInButton = view.findViewById(R.id.credit_in_button)
        creditOutButton = view.findViewById(R.id.credit_out_button)
        validityLayout = view.findViewById(R.id.validity_layout)
        sellRateLayout = view.findViewById(R.id.sell_rate_layout)

        // Set up default dropdowns
        setupDropdown(typeDropdown, listOf("Alfa", "Touch"))
        setupDropdown(validityDropdown, (1..12).map { "${it}M" })

        // Handle button click logic
        creditInButton.setOnClickListener { selectCreditType(true) }
        creditOutButton.setOnClickListener { selectCreditType(false) }
    }

    private fun setupDropdown(dropdown: MaterialAutoCompleteTextView, options: List<String>) {
        dropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options))
    }

    private fun selectCreditType(isCreditIn: Boolean) {
        resetButtonStyles()

        if (isCreditIn) {
            creditInButton.strokeWidth = 5
            creditInButton.strokeColor =
                ContextCompat.getColorStateList(requireContext(), R.color.white)

            validityLayout.visibility = View.VISIBLE
            sellRateLayout.visibility = View.GONE
        } else {
            creditOutButton.strokeWidth = 5
            creditOutButton.strokeColor =
                ContextCompat.getColorStateList(requireContext(), R.color.white)

            validityLayout.visibility = View.GONE
            sellRateLayout.visibility = View.VISIBLE
        }
    }

    private fun resetButtonStyles() {
        creditInButton.strokeWidth = 0
        creditOutButton.strokeWidth = 0
    }

}
