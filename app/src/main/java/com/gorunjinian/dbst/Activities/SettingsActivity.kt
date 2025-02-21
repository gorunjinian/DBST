package com.gorunjinian.dbst.Activities

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.gorunjinian.dbst.MyApplication
import com.gorunjinian.dbst.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val fingerprintToggle = findViewById<Switch>(R.id.fingerprint_toggle)
        val delaySpinner = findViewById<Spinner>(R.id.fingerprint_delay_spinner)

        fingerprintToggle.isChecked = prefs.getBoolean("fingerprint_enabled", false)
        setupDelaySpinner(delaySpinner, prefs)

        fingerprintToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("fingerprint_enabled", isChecked).apply()
            if (isChecked) {
                showBiometricPrompt()
            }
        }

        val dynamicThemeToggle = findViewById<Switch>(R.id.dynamic_theme_toggle)
        val changeThemeButton = findViewById<Button>(R.id.change_theme_button)

        dynamicThemeToggle.isChecked = prefs.getBoolean("dynamic_theming", false)
        dynamicThemeToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dynamic_theming", isChecked).apply()
            restartActivity()
        }

        changeThemeButton.setOnClickListener {
            Toast.makeText(this, "Show Color Picker Dialog", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        val app = application as MyApplication
        if (app.isReopenFromBackground()) {
            app.isAppInBackground = false
            authenticateIfNeeded()
        }
    }

    override fun onPause() {
        super.onPause()
        val app = application as MyApplication
        app.isAppInBackground = true
        app.markPausedTime()
    }

    private fun authenticateIfNeeded() {
        val app = application as MyApplication
        if (app.shouldAuthenticate()) {
            showBiometricPrompt()
        }
    }

    private fun showBiometricPrompt() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                prefs.edit().putLong("last_authenticated_time", System.currentTimeMillis()).apply()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                finish()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Authentication")
            .setSubtitle("Authenticate to access settings")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun setupDelaySpinner(spinner: Spinner, prefs: android.content.SharedPreferences) {
        val delayOptions = (0..30 step 5).map { if (it == 0) "Everytime" else "$it minutes" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, delayOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val savedIndex = prefs.getInt("fingerprint_delay_index", 0)
        spinner.setSelection(savedIndex)

        spinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("fingerprint_delay_index", position)
                    .putInt("fingerprint_delay_value", if (position == 0) 0 else position * 5).apply()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun restartActivity() {
        finish()
        startActivity(intent)
    }
}
