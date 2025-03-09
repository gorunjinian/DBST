package com.gorunjinian.dbst.fabcomponents

import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.data.AppRepository
import com.gorunjinian.dbst.data.UserGivens
import com.gorunjinian.dbst.viewmodels.GivenValuesViewModel
import com.gorunjinian.dbst.viewmodels.ViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manager for the Given Values page in the FAB popup
 */
class GivenValuesManager(private var rootView: View) {
    private val TAG = "GivenValuesManager"

    // ViewModel
    private var viewModel: GivenValuesViewModel? = null

    // UI components
    private lateinit var usdLbpBmrInput: TextInputEditText
    private lateinit var usdLbpVldInput: TextInputEditText
    private lateinit var usdLbpSrInput: TextInputEditText
    private lateinit var vldPriceInput: TextInputEditText
    private lateinit var vldProfitInput: TextInputEditText
    private lateinit var whishBalanceInput: TextInputEditText
    private lateinit var cashLbpBalanceInput: TextInputEditText
    private lateinit var bankBalanceInput: TextInputEditText
    private var btcPriceText: TextView? = null
    private var refreshBtcButton: Button? = null
    private var totalAssetValueText: TextView? = null

    /**
     * Initialize the Given Values manager with repository
     */
    fun initialize(repository: AppRepository? = null) {
        Log.e(TAG, "==================================================")
        Log.e(TAG, "INITIALIZE CALLED IN GIVEN VALUES MANAGER")
        Log.e(TAG, "==================================================")

        try {
            // Initialize UI components first
            initializeUIComponents()

            // Make a direct API call to test connectivity
            testApiDirectly()

            // Get context for ViewModel
            val context = rootView.context
            if (context is FragmentActivity && repository != null) {
                Log.e(TAG, "Context is FragmentActivity and repository is not null")

                try {
                    // Initialize ViewModel using the existing ViewModel class
                    Log.e(TAG, "Creating ViewModel with factory...")
                    val factory = ViewModelFactory(repository)
                    viewModel = ViewModelProvider(context, factory)[GivenValuesViewModel::class.java]
                    Log.e(TAG, "ViewModel created successfully: ${viewModel != null}")

                    // Observe ViewModel data
                    observeViewModel(context)

                    // Show a toast for debugging
                    Toast.makeText(context, "GivenValuesManager initialized", Toast.LENGTH_SHORT).show()

                    // Set up refresh button click listener with extra logging
                    refreshBtcButton?.let { button ->
                        Log.e(TAG, "Setting click listener on refresh button")
                        button.setOnClickListener {
                            Log.e(TAG, "REFRESH BUTTON CLICKED")
                            fetchBtcPriceDirectly()
                            viewModel?.fetchBitcoinPrice()
                        }
                    } ?: Log.e(TAG, "Refresh button is NULL")

                    // Fetch latest Bitcoin price
                    Log.e(TAG, "Triggering fetchBitcoinPrice() through ViewModel")
                    viewModel?.fetchBitcoinPrice()
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing ViewModel: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                Log.e(TAG, "Context is not a FragmentActivity or repository is null")
                Log.e(TAG, "Context is FragmentActivity: ${context is FragmentActivity}")
                Log.e(TAG, "Repository is not null: ${repository != null}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during initialization: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Find and initialize all UI components
     */
    private fun initializeUIComponents() {
        try {
            Log.e(TAG, "Finding UI components")

            // Main text fields
            usdLbpBmrInput = rootView.findViewById(R.id.value1_input)
            usdLbpVldInput = rootView.findViewById(R.id.value2_input)
            usdLbpSrInput = rootView.findViewById(R.id.value3_input)
            vldPriceInput = rootView.findViewById(R.id.value4_input)
            vldProfitInput = rootView.findViewById(R.id.value5_input)
            whishBalanceInput = rootView.findViewById(R.id.value6_input)
            cashLbpBalanceInput = rootView.findViewById(R.id.value7_input)
            bankBalanceInput = rootView.findViewById(R.id.value8_input)

            // Find BTC price components - using null safety
            Log.e(TAG, "Looking for BTC price components")
            btcPriceText = rootView.findViewById(R.id.btc_price_text)
            refreshBtcButton = rootView.findViewById(R.id.refresh_btc_price)
            totalAssetValueText = rootView.findViewById(R.id.total_asset_value)

            // Log whether we found BTC price components
            Log.e(TAG, "BTC price text found: ${btcPriceText != null}")
            Log.e(TAG, "Refresh BTC button found: ${refreshBtcButton != null}")
            Log.e(TAG, "Total asset value text found: ${totalAssetValueText != null}")

            // Special debug - change the text directly
            btcPriceText?.text = "BTC Price (Debug): Testing..."

            Log.e(TAG, "UI components initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing UI components: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Test API directly without using ViewModel
     */
    private fun testApiDirectly() {
        Log.e(TAG, "Testing API directly")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.e(TAG, "Direct API test - connecting to Mempool.space")
                val url = URL("https://mempool.space/api/v1/price")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                Log.e(TAG, "Direct API test - response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.e(TAG, "Direct API test - response: $response")

                    val price = JSONObject(response).getInt("USD")
                    Log.e(TAG, "Direct API test - BTC price: $price")

                    // Update UI on main thread
                    CoroutineScope(Dispatchers.Main).launch {
                        btcPriceText?.text = "BTC Price (Direct): $price"
                        Log.e(TAG, "Updated BTC price text directly: $price")

                        // Show a toast with the price
                        Toast.makeText(rootView.context, "BTC Price: $price", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.e(TAG, "Direct API test - HTTP error: $responseCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Direct API test - exception: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Fetch BTC price directly when refresh button is clicked
     */
    private fun fetchBtcPriceDirectly() {
        Log.e(TAG, "Fetching BTC price directly (from button click)")
        testApiDirectly()
    }

    /**
     * Observe changes to the ViewModel data
     */
    private fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        Log.e(TAG, "Setting up ViewModel observer")
        viewModel?.userGivens?.observe(lifecycleOwner) { userGivens ->
            Log.e(TAG, "UserGivens data updated, refreshing UI")
            updateUIWithUserGivens(userGivens)
        }

        Log.e(TAG, "Observer setup complete")
    }

    /**
     * Update UI components with values from UserGivens
     */
    private fun updateUIWithUserGivens(userGivens: UserGivens) {
        try {
            Log.e(TAG, "Updating UI with UserGivens data")
            Log.e(TAG, "BTC Price in UserGivens: ${userGivens.btcPrice}")

            // Update main fields
            usdLbpBmrInput.setText(userGivens.usdLbpBmr.toString())
            usdLbpVldInput.setText(userGivens.usdLbpVld.toString())
            usdLbpSrInput.setText(userGivens.usdLbpSr.toString())
            vldPriceInput.setText(userGivens.vldPrice.toString())
            vldProfitInput.setText(userGivens.vldProfit.toString())
            whishBalanceInput.setText(userGivens.whishBalance.toString())
            cashLbpBalanceInput.setText(userGivens.cashLbpBalance.toString())
            bankBalanceInput.setText(userGivens.bankBalance.toString())

            // Update BTC price TextView - with extra logging
            if (btcPriceText != null) {
                val priceText = if (userGivens.btcPrice > 0)
                    "$${userGivens.btcPrice}"
                else
                    "Loading..."

                Log.e(TAG, "Updating BTC price text to: $priceText")
                btcPriceText?.text = "BTC Price (USD): $priceText"

                // Also show a toast for debugging
                Toast.makeText(rootView.context, "BTC Price Updated: $priceText", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "Cannot update BTC price text - view is null")
            }

            Log.e(TAG, "UI updated with UserGivens data")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating UI: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Save data from the Given Values page
     */
    fun saveData() {
        Log.e(TAG, "saveData called")

        try {
            // Get current values
            val currentData = viewModel?.userGivens?.value ?: UserGivens()

            // Update with UI values
            val updatedData = currentData.copy(
                usdLbpBmr = usdLbpBmrInput.text.toString().toIntOrNull() ?: 0,
                usdLbpVld = usdLbpVldInput.text.toString().toIntOrNull() ?: 0,
                usdLbpSr = usdLbpSrInput.text.toString().toIntOrNull() ?: 0,
                vldPrice = vldPriceInput.text.toString().toIntOrNull() ?: 0,
                vldProfit = vldProfitInput.text.toString().toDoubleOrNull() ?: 0.0,
                whishBalance = whishBalanceInput.text.toString().toIntOrNull() ?: 0,
                cashLbpBalance = cashLbpBalanceInput.text.toString().toIntOrNull() ?: 0,
                bankBalance = bankBalanceInput.text.toString().toIntOrNull() ?: 0,
                lastUpdated = System.currentTimeMillis()
            )

            // Save to ViewModel
            Log.e(TAG, "Saving updated UserGivens data to ViewModel")
            viewModel?.saveUserGivens(updatedData)

            // Show a toast for debugging
            Toast.makeText(rootView.context, "Data saved", Toast.LENGTH_SHORT).show()

            Log.e(TAG, "User givens data saved")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving data: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Refresh the view reference when recycled
     */
    fun refreshView(newRootView: View) {
        Log.e(TAG, "refreshView called with new root view")
        this.rootView = newRootView
        initializeUIComponents()

        // Special debug - change the text directly
        btcPriceText?.text = "BTC Price (Refresh): Testing..."

        viewModel?.userGivens?.value?.let { updateUIWithUserGivens(it) }
    }
}