package com.gorunjinian.dbst

import android.app.Application
import androidx.fragment.app.FragmentActivity
import android.text.Editable
import android.text.TextWatcher
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.textfield.TextInputEditText
import com.gorunjinian.dbst.data.AppDatabase
import com.gorunjinian.dbst.data.DatabaseInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

class MyApplication : Application() {

    var isAppInBackground = true
    private var lastPausedTime: Long = 0
    private val applicationScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Apply dynamic theming if enabled
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("dynamic_theming", false)) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }

        // Initialize the database with CSV data
        initializeDatabase()
    }

    private fun initializeDatabase() {
        applicationScope.launch {
            val database = AppDatabase.getDatabase(applicationContext)
            DatabaseInitializer.initializeDbIfNeeded(applicationContext, database)
        }
    }

    fun markPausedTime() {
        lastPausedTime = System.currentTimeMillis()
    }

    fun isReopenFromBackground(): Boolean {
        return isAppInBackground
    }

    fun showBiometricPromptIfNeeded(activity: FragmentActivity, onCancelled: () -> Unit = {}) {
        if (!shouldAuthenticate(this)) {
            return
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    prefs.edit().putLong("last_authenticated_time", System.currentTimeMillis())
                        .apply()
                    isAppInBackground = false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onCancelled()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onCancelled()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Authentication")
            .setSubtitle("Authenticate to access the app")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    companion object {

        fun formatNumberWithCommas(editText: TextInputEditText) {
            editText.addTextChangedListener(object : TextWatcher {
                private var current = ""

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s.toString() != current) {
                        editText.removeTextChangedListener(this)

                        val cleanString = s.toString().replace(",", "")
                        if (cleanString.isNotEmpty()) {
                            try {
                                val parsed = cleanString.toDouble()
                                val formatted =
                                    NumberFormat.getNumberInstance(Locale.US).format(parsed)
                                current = formatted
                                editText.setText(formatted)
                                editText.setSelection(formatted.length) // Move cursor to end
                            } catch (e: NumberFormatException) {
                                e.printStackTrace()
                            }
                        }

                        editText.addTextChangedListener(this)
                    }
                }
            })
        }

        fun shouldAuthenticate(myApplication: MyApplication): Boolean {
            val prefs = PreferenceManager.getDefaultSharedPreferences(myApplication)
            val isFingerprintEnabled = prefs.getBoolean("fingerprint_enabled", false)

            if (!isFingerprintEnabled) {
                return false
            }

            // Only consider authentication when reopening from background
            if (!myApplication.isReopenFromBackground()) {
                return false
            }

            val lastAuthenticatedTime = prefs.getLong("last_authenticated_time", 0)
            val currentTime = System.currentTimeMillis()
            val fingerprintDelayIndex = prefs.getInt("fingerprint_delay_index", 0)

            // If index is 0, it means "Everytime", so always authenticate when returning from background
            if (fingerprintDelayIndex == 0) {
                return true
            }

            // For time-based delays, check if enough time has passed since last authentication
            val fingerprintDelayMinutes = fingerprintDelayIndex * 5 // Each index represents 5 minutes
            val fingerprintDelayMillis = fingerprintDelayMinutes * 60 * 1000 // Convert to milliseconds

            return (currentTime - lastAuthenticatedTime) > fingerprintDelayMillis
        }

        fun clearInputFields() {}
    }
}