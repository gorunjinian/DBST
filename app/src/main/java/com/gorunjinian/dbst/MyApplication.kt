package com.gorunjinian.dbst

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.textfield.TextInputEditText
import com.gorunjinian.dbst.data.AppDatabase
import com.gorunjinian.dbst.data.DatabaseInitializer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import kotlin.collections.LinkedHashSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.content.edit

class MyApplication : Application() {

    // Tracks whether the application is in the background or not
    var isAppInBackground = true
        private set

    // Count of resumed activities. If 0, we're in background.
    private var resumedActivityCount = 0

    // A set to track currently active (created) activities
    private val activeActivities: MutableSet<Activity> = Collections.synchronizedSet(LinkedHashSet())

    // Scope for database initialization or other coroutines
    private val applicationScope = CoroutineScope(Dispatchers.Main)

    // Listener to detect changes in dynamic theming
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "dynamic_theming") {
            // If dynamic theming is enabled, reapply dynamic colors
            if (prefs.getBoolean("dynamic_theming", false)) {
                DynamicColors.applyToActivitiesIfAvailable(this)
            }
            // Recreate every active activity so the new theme is applied immediately
            activeActivities.forEach { activity ->
                activity.recreate()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Apply dynamic theming if preference is set
        applyDynamicThemingIfEnabled()

        // Initialize database
        initializeDatabase()

        // Register the SharedPreferences listener
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        // Register activity lifecycle callbacks to track foreground/background transitions
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activeActivities.add(activity)
            }

            override fun onActivityDestroyed(activity: Activity) {
                activeActivities.remove(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                // No-op
            }

            override fun onActivityResumed(activity: Activity) {
                resumedActivityCount++
                // If resumedActivityCount goes from 0 to 1, the app is coming to the foreground
                if (resumedActivityCount == 1) {
                    isAppInBackground = false
                }
            }

            override fun onActivityPaused(activity: Activity) {
                resumedActivityCount--
                // If resumedActivityCount goes from 1 to 0, the app is going to the background
                if (resumedActivityCount == 0) {
                    isAppInBackground = true
                }
            }

            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        })
    }

    /**
     * Applies dynamic theming if the user enabled it in preferences.
     */
    private fun applyDynamicThemingIfEnabled() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("dynamic_theming", false)) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }

    /**
     * Initializes the database in a background thread.
     */
    private fun initializeDatabase() {
        applicationScope.launch {
            val database = AppDatabase.getDatabase(applicationContext)
            DatabaseInitializer.initializeDbIfNeeded(applicationContext, database)
        }
    }

    /**
     * Shows a biometric prompt (fingerprint) if needed. Check for hardware availability
     * and whether the user’s preference requires biometrics.
     */
    fun showBiometricPromptIfNeeded(activity: FragmentActivity, onCancelled: () -> Unit = {}) {
        // 1) Check user preference and background state
        if (!shouldAuthenticate(this)) {
            return
        }

        // 2) Check biometric hardware availability (optional, but recommended)
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            != BiometricManager.BIOMETRIC_SUCCESS
        ) {
            // Hardware not available or not enrolled. Avoid showing the prompt.
            return
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    prefs.edit { putLong("last_authenticated_time", System.currentTimeMillis()) }
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
            }
        )

        // You can customize the prompt further if needed
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Authentication")
            .setSubtitle("Authenticate to access the app")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    companion object {

        fun formatNumberWithCommas(editText: TextInputEditText) {
            editText.addTextChangedListener(object : android.text.TextWatcher {
                private var current = ""

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: android.text.Editable?) {
                    if (s.toString() != current) {
                        editText.removeTextChangedListener(this)

                        val cleanString = s.toString().replace(",", "")
                        if (cleanString.isNotEmpty()) {
                            try {
                                val parsed = cleanString.toDouble()
                                val formatted = NumberFormat.getNumberInstance(Locale.US).format(parsed)
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

            // If fingerprint is disabled, no need to authenticate
            if (!isFingerprintEnabled) {
                return false
            }

            // Only show prompt if the app is re-opened from background
            if (!myApplication.isAppInBackground) {
                return false
            }

            val lastAuthenticatedTime = prefs.getLong("last_authenticated_time", 0)
            val currentTime = System.currentTimeMillis()
            val fingerprintDelayIndex = prefs.getInt("fingerprint_delay_index", 0)

            // If index is 0, it means "Every time", so always authenticate when returning from background
            if (fingerprintDelayIndex == 0) {
                return true
            }

            // Otherwise, each index represents a 5-minute increment
            val fingerprintDelayMinutes = fingerprintDelayIndex * 5
            val fingerprintDelayMillis = fingerprintDelayMinutes * 60 * 1000L

            // Check if enough time has passed since last authentication
            return (currentTime - lastAuthenticatedTime) > fingerprintDelayMillis
        }

        fun setTodayDate(dateInput: TextInputEditText) {
            val today = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            dateInput.setText(dateFormat.format(today.time))
        }
    }
}
