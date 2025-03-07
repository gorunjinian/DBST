package com.gorunjinian.dbst

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.gorunjinian.dbst.data.AppDatabase
import com.gorunjinian.dbst.data.AppRepository
import com.gorunjinian.dbst.data.CashCounter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("StaticFieldLeak", "DefaultLocale", "SetTextI18n")
object FabManager {

    private lateinit var inputFields: List<EditText>
    private lateinit var totalAmountEditText: TextView
    private lateinit var totalAssetValueText: TextView

    private lateinit var viewPager: ViewPager2

    fun setupFab(fab: FloatingActionButton, activity: FragmentActivity) {
        fab.setOnClickListener {
            showPopup(activity) // Always open the popup
        }
    }

    private fun showPopup(context: Context) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.popup_info)

        // Apply rounded background to popup
        val background: Drawable? = ContextCompat.getDrawable(context, R.drawable.rounded_popup)
        dialog.window?.setBackgroundDrawable(background)

        // Ensure popup appears rounded & properly sized
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setDimAmount(0.8f) // Add dim effect to background

        // Create repository
        val database = AppDatabase.getDatabase(context)
        val appDao = database.appDao()
        val repository = AppRepository(appDao)

        // Set up ViewPager and TabLayout
        setupPager(dialog, repository)

        // Set up the Save and Close buttons
        val saveButton: Button = dialog.findViewById(R.id.save_popup)
        val closeButton: Button = dialog.findViewById(R.id.close_popup)

        saveButton.setOnClickListener {
            // Save denomination values when save button is pressed
            saveDenominationValues(repository)
            dialog.dismiss()
            Toast.makeText(context, "New values saved", Toast.LENGTH_SHORT).show()
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Adjust popup window size
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun setupPager(dialog: Dialog, repository: AppRepository) {
        // Initialize ViewPager2
        viewPager = dialog.findViewById(R.id.view_pager)

        // Create page layouts
        val layouts = listOf(
            R.layout.cash_counter_page,
            R.layout.asset_management_page
        )

        // Setup adapter
        val pagerAdapter = PopupPagerAdapter(layouts)
        viewPager.adapter = pagerAdapter

        // Setup page change listener to initialize appropriate page content
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> initCashCounterPage(repository)
                    1 -> initAssetManagementPage()
                }
            }
        })

        // Initialize the first page
        initCashCounterPage(repository)
    }

    private fun initCashCounterPage(repository: AppRepository) {
        // Find the root view of the cash counter page
        val cashCounterView = findViewPagerChildAt(0)

        if (cashCounterView != null) {
            // Initialize input fields
            inputFields = listOf(
                cashCounterView.findViewById(R.id.input_1),
                cashCounterView.findViewById(R.id.input_2),
                cashCounterView.findViewById(R.id.input_5),
                cashCounterView.findViewById(R.id.input_10),
                cashCounterView.findViewById(R.id.input_20),
                cashCounterView.findViewById(R.id.input_50),
                cashCounterView.findViewById(R.id.input_100)
            )

            totalAmountEditText = cashCounterView.findViewById(R.id.total_amount)
            totalAmountEditText.text = "$0"

            // Add text change listeners to update total dynamically
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

            // Load saved denomination values when the page is initialized
            loadSavedDenominationValues(repository)
        }
    }

    private fun initAssetManagementPage() {
        // Find the root view of the asset management page
        val assetManagementView = findViewPagerChildAt(1)

        if (assetManagementView != null) {
            // Initialize total assets text
            totalAssetValueText = assetManagementView.findViewById(R.id.total_asset_value)
            // The assets and total are currently static
        }
    }

    private fun saveDenominationValues(repository: AppRepository) {

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
                val denominationValues = listOf(1, 2, 5, 10, 20, 50, 100)
                updateTotal(denominationValues)
            }
        }
    }

    private fun updateTotal(values: List<Int>) {
        var total = 0
        inputFields.forEachIndexed { index, editText ->
            val input = editText.text.toString()
            if (input.isNotEmpty()) {
                try {
                    total += input.toInt() * values[index]
                } catch (e: NumberFormatException) {
                    // Handle invalid input
                }
            }
        }
        totalAmountEditText.text = "$" + String.format("%,d", total) // Format with commas
    }

     //Helper method to find child views inside ViewPager2
    private fun findViewPagerChildAt(position: Int): View? {
        if (position < 0 || !::viewPager.isInitialized) return null

        // Get the RecyclerView inside ViewPager2
        val recyclerView = viewPager.getChildAt(0) as? RecyclerView ?: return null

        // Find the ViewHolder for the specified position
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) ?: return null

        // Return the itemView
        return viewHolder.itemView
    }
}