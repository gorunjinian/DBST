package com.gorunjinian.dbst.fragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.gorunjinian.dbst.MyApplication.Companion.formatNumberWithCommas
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.data.*
import com.gorunjinian.dbst.viewmodels.ValidityViewModel
import com.gorunjinian.dbst.viewmodels.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import kotlinx.coroutines.delay
import android.app.AlertDialog
import java.util.*


@SuppressLint("SetTextI18s", "SetTextI18n")
class ValidityFragment : Fragment() {

    companion object {
        private const val PERMISSION_REQUEST_SMS = 101
        private const val MAX_CREDIT_PER_SMS = 3.0
    }

    private lateinit var dateInput: TextInputEditText
    private lateinit var personInput: TextInputEditText
    private lateinit var typeDropdown: MaterialAutoCompleteTextView
    private lateinit var validityDropdown: MaterialAutoCompleteTextView
    private lateinit var amountInput: TextInputEditText
    private lateinit var totalInput: TextInputEditText
    private lateinit var rateInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var clearButton: MaterialButton
    private lateinit var creditInButton: MaterialButton
    private lateinit var creditOutButton: MaterialButton
    private lateinit var undoButton: MaterialButton
    private lateinit var validityLayout: TextInputLayout
    private lateinit var totalLayout: TextInputLayout
    private lateinit var rateLayout: TextInputLayout
    private lateinit var creditTypeToggle: MaterialButtonToggleGroup
    private lateinit var saveSendButton: MaterialButton
    private var currentAmount: Double = 0.0

    // ViewModel components
    private lateinit var viewModel: ValidityViewModel
    private lateinit var repository: AppRepository

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
        saveButton = view.findViewById(R.id.save_button)
        clearButton = view.findViewById(R.id.clear_button)
        undoButton = view.findViewById(R.id.undo_button)
        creditInButton = view.findViewById(R.id.credit_in_button)
        creditOutButton = view.findViewById(R.id.credit_out_button)
        creditTypeToggle = view.findViewById(R.id.credit_type_toggle)
        saveSendButton = view.findViewById(R.id.save_send_button)

        // Initialize additional layouts
        validityLayout = view.findViewById(R.id.validity_layout)
        totalLayout = view.findViewById(R.id.total_layout)
        rateLayout = view.findViewById(R.id.rate_layout)

        // Initialize Repository and ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val appDao = database.appDao()
        repository = AppRepository(appDao)
        viewModel =
            ViewModelProvider(this, ViewModelFactory(repository))[ValidityViewModel::class.java]

        // Observe LiveData from ViewModel
        viewModel.lastInsertedVbstIn.observe(viewLifecycleOwner) { entry ->
            lastEntry = entry
            lastEntryType = "credit_in"
            lastEntryTime = System.currentTimeMillis()
            startUndoCountdown()
        }

        viewModel.lastInsertedVbstOut.observe(viewLifecycleOwner) { entry ->
            lastEntry = entry
            lastEntryType = "credit_out"
            lastEntryTime = System.currentTimeMillis()
            startUndoCountdown()
        }

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

        saveSendButton.setOnClickListener {
            // Get the current amount value for later use in SMS
            val amountStr = amountInput.text.toString().replace(",", "")
            if (amountStr.isNotEmpty()) {
                currentAmount = amountStr.toDouble()
                // First check for SMS permission
                if (hasSmsSendPermission()) {
                    // Show SMS dialog if we have permission
                    showSendSmsDialog()
                } else {
                    // Request SMS permission
                    requestSmsSendPermission()
                }
            } else {
                Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        creditTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.credit_in_button -> selectCreditType(isCreditIn = true)
                    R.id.credit_out_button -> selectCreditType(isCreditIn = false)
                }
            }
        }

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
        // Ensure toggle buttons match the selected state
        if (isCreditIn) {
            // Only update the toggle if it doesn't match current selection
            if (creditTypeToggle.checkedButtonId != R.id.credit_in_button) {
                creditTypeToggle.check(R.id.credit_in_button)
            }

            // For Credit IN (VBSTIN): Show validity and total fields, hide rate field
            validityLayout.visibility = View.VISIBLE
            totalLayout.visibility = View.VISIBLE
            rateLayout.visibility = View.GONE

            // Hide Save & Send button for Credit IN
            saveSendButton.visibility = View.GONE
        } else {
            // Only update the toggle if it doesn't match current selection
            if (creditTypeToggle.checkedButtonId != R.id.credit_out_button) {
                creditTypeToggle.check(R.id.credit_out_button)
            }

            // For Credit OUT (VBSTOUT): Show rate field (sell rate), hide validity and total fields
            validityLayout.visibility = View.GONE
            totalLayout.visibility = View.GONE
            rateLayout.visibility = View.VISIBLE

            // Show Save & Send button for Credit OUT
            saveSendButton.visibility = View.VISIBLE
        }
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

        // Determine if we're saving Credit IN or Credit OUT based on toggle selection
        val isCreditIn = creditTypeToggle.checkedButtonId == R.id.credit_in_button

        // Validate common fields
        if (date.isEmpty() || person.isEmpty() || type.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please fill in all required fields",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Validate additional fields based on selected credit type
        if (isCreditIn) {
            // For Credit IN (VBSTIN): require validity and total
            if (validity.isEmpty() || totalStr.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please fill in validity and total fields",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        } else {
            // For Credit OUT (VBSTOUT): require sell rate (entered in rate field)
            if (rateStr.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please fill in the sell rate field",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        // Parse numerical values
        val amount = amountStr.replace(",", "").toDouble()
        if (isCreditIn) {
            // Save record into VBSTIN table using ViewModel
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

            viewModel.insertVbstIn(vbstin)
            Toast.makeText(requireContext(), "Credit IN entry saved", Toast.LENGTH_SHORT).show()
            clearInputFields()
        } else {
            // Save record into VBSTOUT table using ViewModel
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

            viewModel.insertVbstOut(vbstout)
            Toast.makeText(requireContext(), "Credit OUT entry saved", Toast.LENGTH_SHORT).show()
            clearInputFields()
        }
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
                when (lastEntryType) {
                    "credit_in" -> {
                        (lastEntry as? VBSTIN)?.let { vbstin ->
                            viewModel.deleteVbstIn(vbstin)
                            withContext(Dispatchers.Main) {
                                // Repopulate fields for Credit IN
                                selectCreditType(isCreditIn = true) // This now handles toggle selection too
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
                            viewModel.deleteVbstOut(vbstout)
                            withContext(Dispatchers.Main) {
                                // Repopulate fields for Credit OUT
                                selectCreditType(isCreditIn = false) // This now handles toggle selection too
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
                val imm =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(personInput.windowToken, 0)

                // Post a delay to show type dropdown after keyboard is hidden
                typeDropdown.postDelayed({
                    typeDropdown.requestFocus()
                    typeDropdown.showDropDown()
                }, 150)

                true // consume the action
            } else {
                false // don't consume the action
            }
        }

        // For Credit IN mode: Handle transition from type dropdown to validity dropdown
        typeDropdown.setOnItemClickListener { _, _, _, _ ->
            // Only show validity dropdown when Credit IN is selected
            if (creditTypeToggle.checkedButtonId == R.id.credit_in_button) {
                validityDropdown.postDelayed({
                    validityDropdown.requestFocus()
                    validityDropdown.showDropDown()
                }, 150)
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

    private fun hasSmsSendPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsSendPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.SEND_SMS),
            PERMISSION_REQUEST_SMS
        )
    }

    private fun createLoadingDialog(): AlertDialog {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.loading_message)
        messageTextView.text = "Sending SMS messages..."

        return AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }

    // Handle permission result
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_SMS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, show SMS dialog
                showSendSmsDialog()
            } else {
                // Permission denied
                Toast.makeText(
                    requireContext(),
                    "SMS permission denied. Cannot send SMS.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showSendSmsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_send_sms, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        // Get references to views
        val phoneNumberInput = dialogView.findViewById<TextInputEditText>(R.id.phone_number_input)
        val simDropdown = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.sim_dropdown)
        val smsSummary = dialogView.findViewById<TextView>(R.id.sms_summary)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSend = dialogView.findViewById<MaterialButton>(R.id.btn_send)

        // Get available SIMs
        val simOptions = getAvailableSims()
        val simAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            simOptions
        )
        simDropdown.setAdapter(simAdapter)

        // Set default SIM if available
        if (simOptions.isNotEmpty()) {
            simDropdown.setText(simOptions[0], false)
        }

        // Update SMS summary text
        updateSmsSummary(smsSummary, currentAmount)

        // Set up button actions
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSend.setOnClickListener {
            val phoneNumber = phoneNumberInput.text.toString()
            val selectedSim = simDropdown.text.toString()

            if (validateInputs(phoneNumber, selectedSim)) {
                // Dismiss the SMS dialog
                dialog.dismiss()

                // Show loading dialog
                val loadingDialog = createLoadingDialog()
                loadingDialog.show()

                // Launch coroutine to send SMS in background
                lifecycleScope.launch(Dispatchers.IO) {
                    val success = sendSmsMessages(phoneNumber, selectedSim, currentAmount)

                    // Back to the main thread to update UI
                    withContext(Dispatchers.Main) {
                        // Dismiss loading dialog
                        loadingDialog.dismiss()

                        // Show result toast
                        if (success) {
                            Toast.makeText(requireContext(), "All SMS messages sent successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Failed to send some or all SMS messages", Toast.LENGTH_SHORT).show()
                        }

                        // Save the data
                        saveData()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun getAvailableSims(): List<String> {
        try {
            val subscriptionManager =
                requireContext().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                        as? SubscriptionManager

            if (subscriptionManager == null) {
                Log.e("ValidityFragment", "Subscription Manager is null")
                return listOf("Touch SIM", "Alfa SIM")
            }

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("ValidityFragment", "No READ_PHONE_STATE permission")
                return listOf("Touch SIM", "Alfa SIM")
            }

            val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList
            if (activeSubscriptions.isNullOrEmpty()) {
                Log.i("ValidityFragment", "No active subscriptions found")
                return listOf("Touch SIM", "Alfa SIM")
            }

            return activeSubscriptions.map { it.displayName.toString() }
        } catch (e: Exception) {
            Log.e("ValidityFragment", "Error getting SIMs: ${e.message}")
            return listOf("Touch SIM", "Alfa SIM")
        }
    }

    private fun validateInputs(phoneNumber: String, selectedSim: String): Boolean {
        // Check if phone number is exactly 8 digits
        if (phoneNumber.length != 8 || !phoneNumber.all { it.isDigit() }) {
            Toast.makeText(requireContext(), "Please enter a valid 8-digit phone number", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if phone number starts with valid Lebanese prefixes
        val validPrefixes = listOf("03", "70", "71", "72", "75", "76", "79", "81")
        val prefix = phoneNumber.substring(0, 2)

        if (!validPrefixes.contains(prefix)) {
            // Show specific error message listing valid prefixes
            Toast.makeText(
                requireContext(),
                "Invalid Lebanese phone number format.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        if (selectedSim.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a SIM", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun updateSmsSummary(summaryView: TextView, amount: Double) {
        val messageParts = calculateSmsParts(amount)
        val summary = StringBuilder("Will send ${messageParts.size} SMS message(s):\n")

        messageParts.forEachIndexed { index, value ->
            // Format the value the same way for consistency
            val formattedValue = if (value == value.toInt().toDouble()) {
                value.toInt().toString()
            } else {
                value.toString()
            }

            summary.append("SMS ${index + 1}: $formattedValue credit\n")
        }

        summaryView.text = summary.toString()
    }

    private fun calculateSmsParts(amount: Double): List<Double> {
        val parts = mutableListOf<Double>()
        var remainingAmount = amount

        while (remainingAmount > 0) {
            val partValue = when {
                remainingAmount >= MAX_CREDIT_PER_SMS -> MAX_CREDIT_PER_SMS
                remainingAmount >= 2.5 -> 2.5
                remainingAmount >= 2.0 -> 2.0
                remainingAmount >= 1.5 -> 1.5
                remainingAmount >= 1.0 -> 1.0
                remainingAmount >= 0.5 -> 0.5
                else -> remainingAmount // Less than 0.5, use actual value
            }

            parts.add(partValue)
            remainingAmount -= partValue
        }

        return parts
    }

    private suspend fun sendSmsMessages(phoneNumber: String, selectedSim: String, amount: Double): Boolean {
        return try {
            val smsManager = SmsManager.getDefault()
            val messageParts = calculateSmsParts(amount)
            val destinationNumber = if (selectedSim.lowercase().contains("touch")) "1199" else "1313"

            var successCount = 0

            for (value in messageParts) {
                // Format the value to remove decimal if it's a whole number
                val formattedValue = if (value == value.toInt().toDouble()) {
                    value.toInt().toString()
                } else {
                    value.toString()
                }

                val message = "${phoneNumber}t$formattedValue"
                try {
                    smsManager.sendTextMessage(destinationNumber, null, message, null, null)
                    successCount++

                    // Add a small delay between messages to avoid network congestion
                    delay(200)
                } catch (e: Exception) {
                    // Handle individual SMS failure
                    Log.e("SMS_SEND", "Failed to send SMS: ${e.message}")
                }
            }

            successCount == messageParts.size
        } catch (e: Exception) {
            // Handle general SMS failure
            Log.e("SMS_SEND", "Failed to send SMS: ${e.message}")
            false
        }
    }

}