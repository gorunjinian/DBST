package com.gorunjinian.dbst.fragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.google.android.material.materialswitch.MaterialSwitch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.gorunjinian.dbst.MyApplication.Companion.formatNumberWithCommas
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.data.*
import com.gorunjinian.dbst.viewmodels.TetherViewModel
import com.gorunjinian.dbst.viewmodels.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SetTextI18p", "SetTextI18n")
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
    private lateinit var undoButton: MaterialButton
    private lateinit var whishSwitch: MaterialSwitch

    // ViewModel components
    private lateinit var viewModel: TetherViewModel
    private lateinit var repository: AppRepository

    // Undo tracking variables
    private var undoCountDownTimer: CountDownTimer? = null
    private var lastEntryTime: Long = 0L
    private var lastEntry: USDT? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_tether, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Repository and ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val appDao = database.appDao()
        repository = AppRepository(appDao)
        viewModel = ViewModelProvider(this, ViewModelFactory(repository))[TetherViewModel::class.java]

        // Observe LiveData from ViewModel
        viewModel.lastInsertedUsdt.observe(viewLifecycleOwner) { entry ->
            lastEntry = entry
            lastEntryTime = System.currentTimeMillis()
            startUndoCountdown()
        }

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
        undoButton = view.findViewById(R.id.undo_button)
        whishSwitch = view.findViewById(R.id.whish_payment_switch)

        formatNumberWithCommas(cashInput)
        formatNumberWithCommas(usdtAmountInput)

        // Automatically Set Today's Date
        setTodayDate()

        // Improve keyboard navigation between fields
        setupKeyboardNavigation()

        // Automatically select SELL button
        sellButton.isChecked = true

        // Set up Date Picker
        dateInput.setOnClickListener { showDatePicker() }

        // Save Button Logic
        saveButton.setOnClickListener { saveData() }

        clearButton.setOnClickListener { clearInputFields() }

        // Undo Button Logic
        undoButton.setOnClickListener { undoButtonAction() }

        // Disable undo button by default
        undoButton.isEnabled = false
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
        val usdtAmountText = usdtAmountInput.text.toString().replace(",", "")
        val cashText = cashInput.text.toString().replace(",", "")
        val selectedType = when (toggleGroup.checkedButtonId) {
            R.id.buy_button -> "BUY"
            R.id.sell_button -> "SELL"
            else -> ""
        }

        // Validate inputs
        if (date.isEmpty() || person.isEmpty() || usdtAmountText.isEmpty() ||
            cashText.isEmpty() || selectedType.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse numeric values
        val usdtAmount = usdtAmountText.toDouble()
        val cashAmount = cashText.toDouble()

        // Create USDT entry
        val usdtEntry = USDT(
            date = date,
            person = person,
            amountUsdt = usdtAmount,
            amountCash = cashAmount,
            type = selectedType
        )

        // Check if wish payment switch is on
        val isPaidByWhish = whishSwitch.isChecked

        // Save using ViewModel
        viewModel.insertUsdtWithWhishOption(usdtEntry, isPaidByWhish)

        Toast.makeText(
            requireContext(),
            "USDT Transaction Saved: $selectedType" +
                    (if (isPaidByWhish) " (WISH TOPUP)" else ""),
            Toast.LENGTH_SHORT
        ).show()
        clearInputFields()
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

            override fun onFinish() {
                // Once finished, reset the button text and disable it
                undoButton.text = "Undo"
                undoButton.isEnabled = false
                // Reset tracking for last entry
                lastEntryTime = 0L
                lastEntry = null
            }
        }.start()
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
                lastEntry?.let { usdtEntry ->
                    // Use ViewModel to delete
                    viewModel.deleteUsdt(usdtEntry)

                    withContext(Dispatchers.Main) {
                        // Repopulate fields
                        dateInput.setText(usdtEntry.date)
                        personInput.setText(usdtEntry.person)
                        usdtAmountInput.setText(usdtEntry.amountUsdt.toString())
                        cashInput.setText(usdtEntry.amountCash.toString())

                        // Set the correct toggle button based on type
                        when (usdtEntry.type) {
                            "BUY" -> buyButton.isChecked = true
                            "SELL" -> sellButton.isChecked = true
                        }
                    }
                }

                // Clear the tracking variables after undoing
                lastEntryTime = 0L
                lastEntry = null
            }
            Toast.makeText(requireContext(), "Last entry undone", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Undo period expired", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearInputFields() {
        personInput.text?.clear()
        usdtAmountInput.text?.clear()
        cashInput.text?.clear()
    }
}