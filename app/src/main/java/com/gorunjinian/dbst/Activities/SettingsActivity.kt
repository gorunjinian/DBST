package com.gorunjinian.dbst.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.sqlite.db.SimpleSQLiteQuery
import com.gorunjinian.dbst.MyApplication
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.data.AppDao
import com.gorunjinian.dbst.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var tableDeleteSpinner: Spinner
    private lateinit var deleteTableDataButton: Button
    private lateinit var appDao: AppDao

    @SuppressLint("UseSwitchCompatOrMaterialCode")
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

        // Initialize Database
        val database = AppDatabase.getDatabase(this)
        appDao = database.appDao()

        // Initialize Views
        tableDeleteSpinner = findViewById(R.id.table_delete_spinner)
        deleteTableDataButton = findViewById(R.id.delete_table_data_button)

        // Load table names into spinner
        loadTableNames()

        // Set up biometric authentication toggle
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

        // Handle delete button click
        deleteTableDataButton.setOnClickListener {
            confirmAndDeleteTableData()
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
        biometricPrompt =
            BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    prefs.edit().putLong("last_authenticated_time", System.currentTimeMillis())
                        .apply()
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

        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    prefs.edit().putInt("fingerprint_delay_index", position)
                        .putInt(
                            "fingerprint_delay_value",
                            if (position == 0) 0 else position * 5
                        ).apply()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun deleteTableData(tableName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            when (tableName) {
                "DBT" -> {
                    appDao.deleteAllIncome()
                    appDao.resetDbtSequence()
                }
                "DST" -> {
                    appDao.deleteAllExpense()
                    appDao.resetDstSequence()
                }
                "VBSTIN" -> {
                    appDao.deleteAllVbstIn()
                    appDao.resetVbstInSequence()
                }
                "VBSTOUT" -> {
                    appDao.deleteAllVbstOut()
                    appDao.resetVbstOutSequence()
                }
                "USDT" -> {
                    appDao.deleteAllUsdt()
                    appDao.resetUsdtSequence()
                }
                else -> {
                    // Generic table deletion for any other tables
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SettingsActivity,
                            "Generic table deletion not implemented for $tableName",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@SettingsActivity,
                    "Data deleted from $tableName!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmAndDeleteTableData() {
        val selectedTable = tableDeleteSpinner.selectedItem.toString()

        AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete all data from $selectedTable?")
            .setPositiveButton("Yes") { _, _ ->
                deleteTableData(selectedTable)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun restartActivity() {
        finish()
        startActivity(intent)
    }

    private fun loadTableNames() {
        lifecycleScope.launch(Dispatchers.IO) {
            val rawQuery = SimpleSQLiteQuery(
                "SELECT name FROM sqlite_master WHERE type='table' " +
                        "AND name NOT LIKE 'android_metadata' " +
                        "AND name NOT LIKE 'sqlite_sequence' " +
                        "AND name NOT LIKE 'room_master_table'"
            )
            val availableTables = appDao.getAllTableNames(rawQuery)

            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(
                    this@SettingsActivity,
                    android.R.layout.simple_spinner_item,
                    availableTables
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                tableDeleteSpinner.adapter = adapter
            }
        }
    }

    // Add this method to SettingsActivity.kt

    private fun updateSystemBars() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES)

        // Get the primary color from the current theme
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data

        // Ensure window draws behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // FORCE the status and navigation bar colors
        window.statusBarColor = primaryColor
        window.navigationBarColor = primaryColor

        // Handle status bar icons based on theme (dark/light)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isNightMode

        // Handle navigation bar icons
        windowInsetsController.isAppearanceLightNavigationBars = !isNightMode
    }

// Call this in onCreate() and onResume() of SettingsActivity.kt
}