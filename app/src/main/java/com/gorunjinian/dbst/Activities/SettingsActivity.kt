package com.gorunjinian.dbst.activities

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.gorunjinian.dbst.MyApplication
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.ThemeManager
import com.gorunjinian.dbst.data.AppDao
import com.gorunjinian.dbst.data.AppDatabase
import com.gorunjinian.dbst.fragments.ThemeSettingsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    // Add this as a class property to properly track theme change processing
    private var isProcessingThemeChange = false
    // Add a flag to track if any theme-related setting was changed
    private var themeSettingsChanged = false

    private lateinit var tableDeleteDropdown: MaterialAutoCompleteTextView
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var deleteTableDataButton: MaterialButton
    private lateinit var fingerprintToggle: MaterialSwitch
    private lateinit var dynamicThemeToggle: MaterialSwitch
    private lateinit var lightModeToggle: MaterialSwitch
    private lateinit var validityTabToggle: MaterialSwitch
    private lateinit var fingerprintDelayDropdown: MaterialAutoCompleteTextView
    private lateinit var changeThemeButton: MaterialButton
    private lateinit var appInfoButton: MaterialButton
    private lateinit var helpButton: MaterialButton
    private lateinit var aboutDeveloperButton: MaterialButton
    private lateinit var appVersionText: TextView
    private lateinit var appDao: AppDao

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme first based on dynamic colors preference
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val dynamicEnabled = prefs.getBoolean("dynamic_theming", false)

        if (!dynamicEnabled) {
            setTheme(R.style.Theme_DBST)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Set up the TopAppBar
        topAppBar = findViewById(R.id.topAppBar)
        setSupportActionBar(topAppBar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        topAppBar.setNavigationOnClickListener { onBackPressed() }

        // Initialize Database
        val database = AppDatabase.getDatabase(this)
        appDao = database.appDao()

        // Initialize Views
        initializeViews()

        // First, set toggle state WITHOUT triggering the listener
        isProcessingThemeChange = true
        dynamicThemeToggle.isChecked = dynamicEnabled
        isProcessingThemeChange = false

        // Setup dynamic theme toggle
        setupDynamicThemeToggle(prefs)

        // Set up biometric authentication toggle
        fingerprintToggle.isChecked = prefs.getBoolean("fingerprint_enabled", false)
        setupDelayDropdown(prefs)

        fingerprintToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit {
                putBoolean("fingerprint_enabled", isChecked)
                apply()
            }
        }

        // Set up light mode toggle based on current theme mode
        setupLightModeToggle(prefs)

        // Toggle for hiding the "Validity" tab
        setupValidityTabToggle(prefs)

        // Set up theme change button
        changeThemeButton.setOnClickListener {
            // Show theme settings fragment dialog
            val fragment = ThemeSettingsFragment()
            fragment.show(supportFragmentManager, "THEME_SETTINGS")
        }

        // Handle delete button click
        deleteTableDataButton.setOnClickListener {
            confirmAndDeleteTableData()
        }

        // About / Help buttons
        setupAboutAndHelpButtons()

        // Load table names into dropdown
        loadTableNames()

        // Set app version info
        setAppVersionInfo()

        // Reset the theme settings changed flag
        themeSettingsChanged = false
    }

    private fun setupDynamicThemeToggle(prefs: android.content.SharedPreferences) {
        dynamicThemeToggle.setOnCheckedChangeListener { _, isChecked ->
            // Prevent multiple simultaneous theme changes
            if (isProcessingThemeChange) return@setOnCheckedChangeListener

            isProcessingThemeChange = true
            themeSettingsChanged = true

            // Update the preference immediately
            prefs.edit {
                putBoolean("dynamic_theming", isChecked)
                apply()
            }

            // Use Handler for reliable execution after UI operations
            Handler(Looper.getMainLooper()).postDelayed({
                // Reset the lock before recreating to avoid issues
                isProcessingThemeChange = false

                // Always set the recreation flag for MainActivity
                prefs.edit {
                    putBoolean("needs_recreation", true)
                    apply()
                }

                // Recreate this activity to apply theme change
                recreate()
            }, 200) // Slightly longer delay to ensure UI has time to settle
        }
    }

    private fun setupLightModeToggle(prefs: android.content.SharedPreferences) {
        // Determine current mode
        val currentThemeMode = prefs.getString("theme_mode", ThemeManager.THEME_MODE_SYSTEM)
            ?: ThemeManager.THEME_MODE_SYSTEM

        // Set initial state
        lightModeToggle.isChecked = currentThemeMode == ThemeManager.THEME_MODE_LIGHT

        lightModeToggle.setOnCheckedChangeListener { _, isChecked ->
            // Mark that theme settings have changed
            themeSettingsChanged = true

            // Set the theme mode based on the toggle
            val newThemeMode = if (isChecked) ThemeManager.THEME_MODE_LIGHT else ThemeManager.THEME_MODE_DARK

            // Use ThemeManager to change theme mode
            ThemeManager.setThemeMode(applicationContext, newThemeMode)

            // Save preference
            prefs.edit {
                putString("theme_mode", newThemeMode)
                apply()
            }

            // Set flag for MainActivity to recreate
            prefs.edit {
                putBoolean("needs_recreation", true)
                apply()
            }
        }
    }

    private fun setupValidityTabToggle(prefs: android.content.SharedPreferences) {
        validityTabToggle.isChecked = prefs.getBoolean("turn_off_validity_tab", false)
        validityTabToggle.setOnCheckedChangeListener { _, isChecked ->
            // Mark that UI settings have changed and need recreation
            themeSettingsChanged = true

            prefs.edit {
                putBoolean("turn_off_validity_tab", isChecked)
                apply()
            }

            // Set flag for MainActivity to recreate to update tab visibility
            prefs.edit {
                putBoolean("needs_recreation", true)
                apply()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val app = application as MyApplication
        // If the app was previously in background, show biometric prompt if needed
        if (app.isAppInBackground) {
            app.showBiometricPromptIfNeeded(this) {
                // This is called if authentication is cancelled or fails
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // Only set recreation flag if theme settings were actually changed
        if (themeSettingsChanged) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            prefs.edit {
                putBoolean("needs_recreation", true)
                apply()
            }
        }
    }

    @Deprecated("Use onBackPressedDispatcher instead")
    override fun onBackPressed() {
        // Only set recreation flag if theme settings were actually changed
        if (themeSettingsChanged) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            prefs.edit {
                putBoolean("needs_recreation", true)
                apply()
            }
        }
        super.onBackPressed()
        // Using default transition when going back
    }

    private fun initializeViews() {
        tableDeleteDropdown = findViewById(R.id.table_delete_dropdown)
        deleteTableDataButton = findViewById(R.id.delete_table_data_button)
        fingerprintToggle = findViewById(R.id.fingerprint_toggle)
        dynamicThemeToggle = findViewById(R.id.dynamic_theme_toggle)
        lightModeToggle = findViewById(R.id.light_mode_toggle)
        validityTabToggle = findViewById(R.id.validity_tab_toggle)
        fingerprintDelayDropdown = findViewById(R.id.fingerprint_delay_dropdown)
        changeThemeButton = findViewById(R.id.change_theme_button)
        appInfoButton = findViewById(R.id.app_info_button)
        helpButton = findViewById(R.id.help_button)
        aboutDeveloperButton = findViewById(R.id.about_developer_button)
        appVersionText = findViewById(R.id.app_version_text)
    }

    private fun setupDelayDropdown(prefs: android.content.SharedPreferences) {
        val delayOptions = (0..30 step 5).map { if (it == 0) "Every time" else "$it minutes" }
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, delayOptions)
        fingerprintDelayDropdown.setAdapter(adapter)

        val savedIndex = prefs.getInt("fingerprint_delay_index", 0)
        fingerprintDelayDropdown.setText(delayOptions[savedIndex], false)

        fingerprintDelayDropdown.setOnItemClickListener { _, _, position, _ ->
            prefs.edit {
                putInt("fingerprint_delay_index", position)
                putInt("fingerprint_delay_value", if (position == 0) 0 else position * 5)
                apply()
            }
        }
    }

    private fun confirmAndDeleteTableData() {
        val selectedTable = tableDeleteDropdown.text.toString()

        if (selectedTable.isEmpty()) {
            Toast.makeText(this, "Please select a table first", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete all data from $selectedTable?")
            .setPositiveButton("Yes") { _, _ ->
                deleteTableData(selectedTable)
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun loadTableNames() {
        lifecycleScope.launch(Dispatchers.IO) {
            val rawQuery = SimpleSQLiteQuery(
                "SELECT name FROM sqlite_master WHERE type='table' " +
                        "AND name NOT LIKE 'android_metadata' " +
                        "AND name NOT LIKE 'sqlite_sequence' " +
                        "AND name NOT LIKE 'cash_counter' " +
                        "AND name NOT LIKE 'user_givens' " +
                        "AND name NOT LIKE 'room_master_table'"
            )
            val availableTables = appDao.getAllTableNames(rawQuery)

            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(
                    this@SettingsActivity,
                    R.layout.dropdown_item,
                    availableTables
                )
                tableDeleteDropdown.setAdapter(adapter)
                if (availableTables.isNotEmpty()) {
                    tableDeleteDropdown.setText(availableTables[0], false)
                }
            }
        }
    }

    private fun setupAboutAndHelpButtons() {
        appInfoButton.setOnClickListener {
            showAppInfoDialog()
        }

        helpButton.setOnClickListener {
            showHelpDialog()
        }

        aboutDeveloperButton.setOnClickListener {
            showDeveloperInfoDialog()
        }
    }

    private fun showAppInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle("About DBST")
            .setMessage(
                "DBST (Dollar Buy Sell Transactions) is a financial tracking app " +
                        "designed to help you manage personal finances, track income, " +
                        "expenses, and currency exchanges."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("Help & Support")
            .setMessage(
                "For assistance with using DBST, please contact the developer " +
                        "or check the documentation in the About Developer section."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDeveloperInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle("About Developer")
            .setMessage(
                "Developed by Gorun Jinian\n" +
                        "Haigazian University\n" +
                        "Spring Semester 2024-2025\n" +
                        "Beirut, Lebanon"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun setAppVersionInfo() {
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val version = pInfo.versionName
            appVersionText.text = "DBST v$version"
        } catch (e: PackageManager.NameNotFoundException) {
            appVersionText.text = "DBST"
        }
    }
}