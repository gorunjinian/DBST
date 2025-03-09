package com.gorunjinian.dbst.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorunjinian.dbst.data.AppRepository
import com.gorunjinian.dbst.data.UserGivens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * ViewModel for managing Given Values data
 */
class GivenValuesViewModel(private val repository: AppRepository) : ViewModel() {
    private val TAG = "GivenValuesViewModel"

    // LiveData for UserGivens
    private val _userGivens = MutableLiveData<UserGivens>()
    val userGivens: LiveData<UserGivens> = _userGivens

    // LiveData for errors
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Initialize with default values
    init {
        _userGivens.value = UserGivens()
        loadUserGivens()
    }

    // Load UserGivens from repository
    fun loadUserGivens() {
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    repository.getUserGivens()
                }
                _userGivens.value = data ?: UserGivens() // Use default if null
                Log.d(TAG, "Loaded user givens data: ${data != null}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user givens: ${e.message}", e)
                _errorMessage.value = "Failed to load data: ${e.message}"
            }
        }
    }

    // Save UserGivens to repository
    fun saveUserGivens(data: UserGivens) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.insertUserGivens(data)
                }
                _userGivens.value = data
                Log.d(TAG, "Saved user givens data successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving user givens: ${e.message}", e)
                _errorMessage.value = "Failed to save data: ${e.message}"
            }
        }
    }

    // Fetch Bitcoin price from Mempool.space API
    fun fetchBitcoinPrice() {
        Log.d(TAG, "Fetching Bitcoin price started")
        viewModelScope.launch {
            try {
                val price = withContext(Dispatchers.IO) {
                    fetchBtcPriceFromApi()
                }

                Log.d(TAG, "Got Bitcoin price from API: $price")

                if (price > 0) {
                    // Update the BTC price in UserGivens while keeping other values
                    val currentData = _userGivens.value ?: UserGivens()
                    val updatedData = currentData.copy(
                        btcPrice = price,
                        lastUpdated = System.currentTimeMillis()
                    )

                    Log.d(TAG, "Updating UserGivens with BTC price: $price")
                    _userGivens.value = updatedData

                    // Save updated data to repository
                    withContext(Dispatchers.IO) {
                        repository.insertUserGivens(updatedData)
                    }
                    Log.d(TAG, "Saved updated UserGivens with new BTC price")
                } else {
                    Log.w(TAG, "Received invalid BTC price (0 or negative)")
                    _errorMessage.value = "Received invalid BTC price from API"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Bitcoin price: ${e.message}", e)
                _errorMessage.value = "Failed to fetch Bitcoin price: ${e.message}"
            }
        }
    }

    // Helper method to fetch BTC price from API
    private suspend fun fetchBtcPriceFromApi(): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Connecting to Mempool.space API")
                val url = URL("https://mempool.space/api/v1/prices")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                Log.d(TAG, "API response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "API response: $response")

                    val jsonObject = JSONObject(response)

                    // Parse the USD price from the response
                    val usdPrice = jsonObject.getInt("USD")
                    Log.d(TAG, "Parsed USD price: $usdPrice")
                    usdPrice
                } else {
                    Log.e(TAG, "HTTP error code: $responseCode")
                    0 // Return 0 on error
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching BTC price: ${e.message}", e)
                0 // Return 0 on error
            }
        }
    }
}