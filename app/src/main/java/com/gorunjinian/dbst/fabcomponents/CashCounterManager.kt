package com.gorunjinian.dbst.fabcomponents

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.data.AppRepository
import com.gorunjinian.dbst.data.CashCounter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manager for the Cash Counter page in the FAB popup
 * Handles denomination inputs, calculation, and persistence
 */
class CashCounterManager(private val rootView: View) {

    // UI components
    private lateinit var inputFields: List<EditText>
    private lateinit var totalAmountEditText: TextView

    // Denominations corresponding to each input field
    private val denominationValues = listOf(1, 2, 5, 10, 20, 50, 100)

    /**
     * Initialize the Cash Counter manager
     */
    fun initialize(repository: AppRepository) {
        // Initialize input fields
        inputFields = listOf(
            rootView.findViewById(R.id.input_1),
            rootView.findViewById(R.id.input_2),
            rootView.findViewById(R.id.input_5),
            rootView.findViewById(R.id.input_10),
            rootView.findViewById(R.id.input_20),
            rootView.findViewById(R.id.input_50),
            rootView.findViewById(R.id.input_100)
        )

        totalAmountEditText = rootView.findViewById(R.id.total_amount)
        totalAmountEditText.text = "$0"

        // Add text change listeners to update total dynamically
        inputFields.forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    updateTotal()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        // Load saved denomination values
        loadSavedDenominationValues(repository)
    }

    /**
     * Update the total amount based on the current input values
     */
    @SuppressLint("DefaultLocale")
    private fun updateTotal() {
        var total = 0
        inputFields.forEachIndexed { index, editText ->
            val input = editText.text.toString()
            if (input.isNotEmpty()) {
                try {
                    total += input.toInt() * denominationValues[index]
                } catch (e: NumberFormatException) {
                    // Handle invalid input
                }
            }
        }
        totalAmountEditText.text = "$" + String.format("%,d", total) // Format with commas
    }

    /**
     * Save the current denomination values to the database
     */
    fun saveData(repository: AppRepository) {
        // Parse values, explicitly converting empty string to 0
        val ones = inputFields[0].text.toString().let { it.ifEmpty { "0" } }.toInt()
        val twos = inputFields[1].text.toString().let { it.ifEmpty { "0" } }.toInt()
        val fives = inputFields[2].text.toString().let { it.ifEmpty { "0" } }.toInt()
        val tens = inputFields[3].text.toString().let { it.ifEmpty { "0" } }.toInt()
        val twenties = inputFields[4].text.toString().let { it.ifEmpty { "0" } }.toInt()
        val fifties = inputFields[5].text.toString().let { it.ifEmpty { "0" } }.toInt()
        val hundreds = inputFields[6].text.toString().let { it.ifEmpty { "0" } }.toInt()

        // Create entity and save to database
        val cashCounter = CashCounter(
            id = 1, // Always use ID 1 for the single instance
            ones = ones,
            twos = twos,
            fives = fives,
            tens = tens,
            twenties = twenties,
            fifties = fifties,
            hundreds = hundreds
        )

        // Launch coroutine to save data using the repository instance
        CoroutineScope(Dispatchers.IO).launch {
            repository.saveCashCounter(cashCounter)
        }
    }

    /**
     * Load saved denomination values from the database
     */
    private fun loadSavedDenominationValues(repository: AppRepository) {
        CoroutineScope(Dispatchers.Main).launch {
            val cashCounter = withContext(Dispatchers.IO) {
                repository.getCashCounter()
            }

            // If data exists, populate the input fields
            cashCounter?.let { counter ->
                inputFields[0].setText(counter.ones.toString())
                inputFields[1].setText(counter.twos.toString())
                inputFields[2].setText(counter.fives.toString())
                inputFields[3].setText(counter.tens.toString())
                inputFields[4].setText(counter.twenties.toString())
                inputFields[5].setText(counter.fifties.toString())
                inputFields[6].setText(counter.hundreds.toString())

                // Update the total
                updateTotal()
            }
        }
    }
}