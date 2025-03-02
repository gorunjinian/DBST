package com.gorunjinian.dbst.fragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

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
    private lateinit var clearButton: MaterialButton
    private lateinit var undoButton: MaterialButton
    private lateinit var validityLayout: TextInputLayout
    private lateinit var totalLayout: TextInputLayout
    private lateinit var rateLayout: TextInputLayout

    // Database components
    private lateinit var database: AppDatabase
    private lateinit var appDao: AppDao

    // Undo tracking variables
    private var undoCountDownTimer: CountDownTimer? = null
    private var lastEntryTime: Long = 0L
    private var lastEntryType: String? = null  // "credit_in" or "credit_out"
    private var lastEntry: Any? = null         // holds the last inserted VBSTIN or VBSTOUT instance

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
        clearButton = view.findViewById(R.id.clear_button)
        undoButton = view.findViewById(R.id.undo_button)

        // Initialize additional layouts
        validityLayout = view.findViewById(R.id.validity_layout)
        totalLayout = view.findViewById(R.id.total_layout)
        rateLayout = view.findViewById(R.id.rate_layout)


        // Initialize Database and DAO
        database = AppDatabase.getDatabase(requireContext())
        appDao = database.appDao()

        formatNumberWithCommas(amountInput)
        formatNumberWithCommas(totalInput)
        formatNumberWithCommas(rateInput)

        // Automatically set today's date
        setTodayDate()

        // Setup auto-open for dropdowns
        setupAutoOpenDropdowns()

        // Set default values
        rateInput.setText("0")
        totalInput.setText("0")

        // Set up Date Picker
        dateInput.setOnClickListener { showDatePicker() }

        // Set up default dropdown options
        setupDropdown(typeDropdown, listOf("Alfa", "Touch"))
        setupDropdown(validityDropdown, (1..12).map { "${it}M" })

        // Ensure clicking dropdown shows the list
        typeDropdown.setOnClickListener { typeDropdown.showDropDown() }
        validityDropdown.setOnClickListener { validityDropdown.showDropDown() }

        // Clear button functionality
        clearButton.setOnClickListener { clearInputFields() }

        // Set up credit type button click listeners
        creditInButton.setOnClickListener { selectCreditType(isCreditIn = true) }
        creditOutButton.setOnClickListener { selectCreditType(isCreditIn = false) }

        // Set Credit OUT as default selection
        selectCreditType(isCreditIn = false)

        // Set up Undo button click listener
        undoButton.setOnClickListener { undoButtonAction() }
        // Initially disable undo button
        undoButton.isEnabled = false

        // Save button logic
        saveButton.setOnClickListener { saveData() }
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
            // For Credit IN (VBSTIN): Show validity and total fields, hide rate field.
            creditInButton.strokeWidth = 10
            creditInButton.strokeColor =
                ColorStateList.valueOf(
                    MaterialColors.getColor(creditInButton, com.google.android.material.R.attr.colorOnBackground)
                )

            validityLayout.visibility = View.VISIBLE
            totalLayout.visibility = View.VISIBLE
            rateLayout.visibility = View.GONE
        } else {
            // For Credit OUT (VBSTOUT): Show rate field (sell rate), hide validity and total fields.
            creditOutButton.strokeWidth = 10
            creditOutButton.strokeColor =
                ColorStateList.valueOf(
                    MaterialColors.getColor(creditOutButton, com.google.android.material.R.attr.colorOnBackground)
                )

            validityLayout.visibility = View.GONE
            totalLayout.visibility = View.GONE
            rateLayout.visibility = View.VISIBLE
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
        val amountStr = amountInput.text.toString()
        val rateStr = rateInput.text.toString()
        val validity = validityDropdown.text.toString()
        val totalStr = totalInput.text.toString()

        // Validate common fields
        if (date.isEmpty() || person.isEmpty() || type.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate additional fields based on selected credit type
        if (creditInButton.strokeWidth > 0) {
            // For Credit IN (VBSTIN): require validity and total
            if (validity.isEmpty() || totalStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in validity and total fields", Toast.LENGTH_SHORT).show()
                return
            }
        } else if (creditOutButton.strokeWidth > 0) {
            // For Credit OUT (VBSTOUT): require sell rate (entered in rate field)
            if (rateStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in the sell rate field", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Parse numerical values
        val amount = amountStr.replace(",", "").toDouble()
        if (creditInButton.strokeWidth > 0) {
            // Save record into VBSTIN table.
            val total = totalStr.replace(",", "").toDouble()
            // VBSTIN computes rate automatically as total/amount.
            val vbstin = VBSTIN(
                date = date,
                person = person,
                type = type,
                validity = validity,
                amount = amount,
                total = total
            )
            lifecycleScope.launch {
                val newId = appDao.insertVbstIn(vbstin)
                val savedVbstin = vbstin.copy(id = newId.toInt())
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Credit IN entry saved", Toast.LENGTH_SHORT).show()
                    clearInputFields()
                }
                // Track the last entry for undo
                lastEntryTime = System.currentTimeMillis()
                lastEntryType = "credit_in"
                lastEntry = savedVbstin
                startUndoCountdown()
            }
        } else if (creditOutButton.strokeWidth > 0) {
            // Save record into VBSTOUT table.
            val sellrate = rateStr.replace(",", "").toDouble()
            val profit = 0.0 // Set profit to 0.0 or compute as needed
            val vbstout = VBSTOUT(
                date = date,
                person = person,
                amount = amount,
                sellrate = sellrate,
                type = type,
                profit = profit
            )
            lifecycleScope.launch {
                val newId = appDao.insertVbstOut(vbstout)
                val savedVbstout = vbstout.copy(id = newId.toInt())
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Credit OUT entry saved", Toast.LENGTH_SHORT).show()
                    clearInputFields()
                }
                // Track the last entry for undo
                lastEntryTime = System.currentTimeMillis()
                lastEntryType = "credit_out"
                lastEntry = savedVbstout
                startUndoCountdown()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun undoButtonAction() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEntryTime <= 30000) {  // within 30 seconds
            // Cancel the countdown so it stops updating the button text
            undoCountDownTimer?.cancel()
            // Reset button text immediately and disable it
            undoButton.text = "Undo"
            undoButton.isEnabled = false

            lifecycleScope.launch {
                when (lastEntryType) {
                    "credit_in" -> {
                        (lastEntry as? VBSTIN)?.let { vbstin ->
                            appDao.deleteVbstIn(vbstin.id)
                            withContext(Dispatchers.Main) {
                                // Repopulate fields for Credit IN
                                selectCreditType(isCreditIn = true)
                                dateInput.setText(vbstin.date)
                                personInput.setText(vbstin.person)
                                typeDropdown.setText(vbstin.type, false)
                                validityDropdown.setText(vbstin.validity, false)
                                amountInput.setText(vbstin.amount.toString())
                                totalInput.setText(vbstin.total.toString())
                            }
                        }
                    }
                    "credit_out" -> {
                        (lastEntry as? VBSTOUT)?.let { vbstout ->
                            appDao.deleteVbstOut(vbstout.id)
                            withContext(Dispatchers.Main) {
                                // Repopulate fields for Credit OUT
                                selectCreditType(isCreditIn = false)
                                dateInput.setText(vbstout.date)
                                personInput.setText(vbstout.person)
                                typeDropdown.setText(vbstout.type, false)
                                amountInput.setText(vbstout.amount.toString())
                                rateInput.setText(vbstout.sellrate.toString())
                            }
                        }
                    }
                }
                // Clear the tracking variables after undoing
                lastEntryTime = 0L
                lastEntryType = null
                lastEntry = null
            }
            Toast.makeText(requireContext(), "Last entry undone", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Undo period expired", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startUndoCountdown() {
        // Cancel any previous countdown
        undoCountDownTimer?.cancel()

        // Enable the undo button and set initial text
        undoButton.isEnabled = true
        undoButton.text = "Undo (30s)"

        // Create and start a new CountDownTimer for 30 seconds
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
                lastEntryType = null
                lastEntry = null
            }
        }.start()
    }

    private fun setupAutoOpenDropdowns() {
        // Handle keyboard to dropdown transition from person to type dropdown
        personInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                // Hide keyboard
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(personInput.windowToken, 0)

                // Post a delay to show type dropdown after keyboard is hidden
                typeDropdown.postDelayed({
                    typeDropdown.requestFocus()
                    typeDropdown.showDropDown()
                }, 200)

                true // consume the action
            } else {
                false // don't consume the action
            }
        }

        // For Credit IN mode: Handle transition from type dropdown to validity dropdown
        typeDropdown.setOnItemClickListener { _, _, _, _ ->
            // Only show validity dropdown when Credit IN is selected
            if (creditInButton.strokeWidth > 0) {
                validityDropdown.postDelayed({
                    validityDropdown.requestFocus()
                    validityDropdown.showDropDown()
                }, 200)
            }
        }
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