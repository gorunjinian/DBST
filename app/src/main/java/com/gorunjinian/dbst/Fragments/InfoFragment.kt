package com.gorunjinian.dbst.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gorunjinian.dbst.R
import com.google.android.material.textfield.TextInputEditText

class InfoFragment : Fragment() {

    private lateinit var inputFields: List<TextInputEditText>
    private lateinit var totalAmountEditText: TextInputEditText  // Changed from MaterialTextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_info, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize input fields
        inputFields = listOf(
            view.findViewById(R.id.input_1),
            view.findViewById(R.id.input_2),
            view.findViewById(R.id.input_5),
            view.findViewById(R.id.input_10),
            view.findViewById(R.id.input_20),
            view.findViewById(R.id.input_50),
            view.findViewById(R.id.input_100)
        )

        totalAmountEditText = view.findViewById(R.id.total_amount)  // Corrected


        totalAmountEditText.setText("$0")

        // Add text change listeners to update total
        val denominationValues = listOf(1, 2, 5, 10, 20, 50, 100)
        inputFields.forEachIndexed { _, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    updateTotal(denominationValues)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }


    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun updateTotal(values: List<Int>) {
        var total = 0
        inputFields.forEachIndexed { index, editText ->
            val input = editText.text.toString()
            if (input.isNotEmpty()) {
                total += input.toInt() * values[index]
            }
        }
        totalAmountEditText.setText("$" + String.format("%,d", total))
    }

}
