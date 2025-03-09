package com.gorunjinian.dbst.fabcomponents

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
 */
class CashCounterManager(private var rootView: View) {
    private val TAG = "CashCounterManager"

    // UI components
    private lateinit var inputFields: List<EditText>
    private lateinit var totalAmountEditText: TextView

    // Denominations
    private val denominationValues = listOf(1, 2, 5, 10, 20, 50, 100)

    // Current cash counter state
    private var currentCashCounter: CashCounter? = null

    fun loadData(repository: AppRepository) {
        Log.d(TAG, "Loading cash counter data")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cashCounter = repository.getCashCounter()
                Log.d(TAG, "Loaded cash counter: $cashCounter")

                withContext(Dispatchers.Main) {
                    if (cashCounter != null) {
                        currentCashCounter = cashCounter
                        updateUIWithCashCounter(cashCounter)
                    } else {
                        Log.d(TAG, "No cash counter data found")
                        clearInputFields()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cash counter data: ${e.message}", e)
            }
        }
    }

    private fun updateUIWithCashCounter(cashCounter: CashCounter) {
        Log.d(TAG, "Updating UI with cash counter: $cashCounter")

        try {
            // Set field values
            inputFields[0].setText(if (cashCounter.ones > 0) cashCounter.ones.toString() else "")
            inputFields[1].setText(if (cashCounter.twos > 0) cashCounter.twos.toString() else "")
            inputFields[2].setText(if (cashCounter.fives > 0) cashCounter.fives.toString() else "")
            inputFields[3].setText(if (cashCounter.tens > 0) cashCounter.tens.toString() else "")
            inputFields[4].setText(if (cashCounter.twenties > 0) cashCounter.twenties.toString() else "")
            inputFields[5].setText(if (cashCounter.fifties > 0) cashCounter.fifties.toString() else "")
            inputFields[6].setText(if (cashCounter.hundreds > 0) cashCounter.hundreds.toString() else "")

            // Update total
            updateTotal()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating UI with cash counter: ${e.message}", e)
        }
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun updateTotal() {
        var total = 0

        try {
            for (i in inputFields.indices) {
                val editText = inputFields[i]
                val input = editText.text.toString()

                if (input.isNotEmpty()) {
                    try {
                        total += input.toInt() * denominationValues[i]
                    } catch (e: NumberFormatException) {
                        Log.e(TAG, "Invalid number: $input")
                    }
                }
            }

            totalAmountEditText.text = "$" + String.format("%,d", total)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating total: ${e.message}", e)
        }
    }

    fun refreshView(newRootView: View) {
        // Store the reference to the new view
        val oldRootView = this.rootView

        // Only process if the view is actually different
        if (oldRootView !== newRootView) {
            Log.d(TAG, "Refreshing CashCounterManager view")

            // Update the root view field - needs to be mutable
            // You'll need to change the field declaration from:
            // private val rootView: View
            // to:
            // private var rootView: View

            this.rootView = newRootView

            // Reinitialize the UI components
            initializeUI()
        }
    }

    private fun initializeUI() {
        try {
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

            // Add text change listeners
            for (i in inputFields.indices) {
                val editText = inputFields[i]
                editText.removeTextChangedListeners()
                editText.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        updateTotal()
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }

            // If we have cached data, update UI
            currentCashCounter?.let {
                updateUIWithCashCounter(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing CashCounterManager UI: ${e.message}", e)
        }
    }

    // 3. Modify the initialize method to use initializeUI
    fun initialize(repository: AppRepository) {
        Log.d(TAG, "Initializing CashCounterManager")

        try {
            // Initialize UI components
            initializeUI()

            // Load data
            loadData(repository)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing CashCounterManager: ${e.message}", e)
        }
    }

    fun saveData(repository: AppRepository) {
        Log.d(TAG, "Saving cash counter data")

        try {
            // Parse values
            val ones = inputFields[0].text.toString().let { it.ifEmpty { "0" } }.toInt()
            val twos = inputFields[1].text.toString().let { it.ifEmpty { "0" } }.toInt()
            val fives = inputFields[2].text.toString().let { it.ifEmpty { "0" } }.toInt()
            val tens = inputFields[3].text.toString().let { it.ifEmpty { "0" } }.toInt()
            val twenties = inputFields[4].text.toString().let { it.ifEmpty { "0" } }.toInt()
            val fifties = inputFields[5].text.toString().let { it.ifEmpty { "0" } }.toInt()
            val hundreds = inputFields[6].text.toString().let { it.ifEmpty { "0" } }.toInt()

            // Create entity
            val cashCounter = CashCounter(
                id = 1,
                ones = ones,
                twos = twos,
                fives = fives,
                tens = tens,
                twenties = twenties,
                fifties = fifties,
                hundreds = hundreds,
                lastUpdated = System.currentTimeMillis()
            )

            Log.d(TAG, "Saving cash counter: $cashCounter")

            // Save to database
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.saveCashCounter(cashCounter)
                    currentCashCounter = cashCounter
                    Log.d(TAG, "Cash counter saved successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving cash counter: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing cash counter data: ${e.message}", e)
        }
    }

    private fun clearInputFields() {
        try {
            inputFields.forEach { it.setText("") }
            updateTotal()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing input fields: ${e.message}", e)
        }
    }

    /**
     * Remove all text changed listeners from an EditText
     */
    @SuppressLint("DiscouragedPrivateApi")
    private fun EditText.removeTextChangedListeners() {
        // Android doesn't provide a direct way to remove listeners,
        // but we can get the private listeners field using reflection
        try {
            val field = TextView::class.java.getDeclaredField("mListeners")
            field.isAccessible = true
            val listeners = field.get(this) as? ArrayList<*>
            listeners?.clear()
        } catch (e: Exception) {
            // If reflection fails, just ignore
            Log.e(TAG, "Failed to remove text changed listeners: ${e.message}")
        }
    }
}