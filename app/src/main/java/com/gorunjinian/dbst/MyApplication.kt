package com.gorunjinian.dbst

import android.app.Application
import android.text.Editable
import android.text.TextWatcher
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

    fun shouldAuthenticate(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isFingerprintEnabled = prefs.getBoolean("fingerprint_enabled", false)
        val lastAuthenticatedTime = prefs.getLong("last_authenticated_time", 0)
        val currentTime = System.currentTimeMillis()
        val fingerprintDelay =
            prefs.getInt("fingerprint_delay_value", 0) * 60 * 1000 // Convert minutes to ms

        return isFingerprintEnabled && (currentTime - lastAuthenticatedTime > fingerprintDelay)
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
    }
}
