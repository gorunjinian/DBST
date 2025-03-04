package com.gorunjinian.dbst.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.gorunjinian.dbst.MyApplication
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.data.AppDao
import com.gorunjinian.dbst.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@SuppressLint("SetTextI18n","UseSwitchCompatOrMaterialCode")
class SettingsActivity : AppCompatActivity() {

    private lateinit var tableDeleteDropdown: MaterialAutoCompleteTextView
    private lateinit var deleteTableDataButton: MaterialButton
    private lateinit var fingerprintToggle: MaterialSwitch
    private lateinit var dynamicThemeToggle: MaterialSwitch
    private lateinit var fingerprintDelayDropdown: MaterialAutoCompleteTextView
    private lateinit var changeThemeButton: MaterialButton
    private lateinit var appInfoButton: MaterialButton
    private lateinit var helpButton: MaterialButton
    private lateinit var aboutDeveloperButton: MaterialButton
    private lateinit var appVersionText: TextView
    private lateinit var appDao: AppDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Initialize Database
        val database = AppDatabase.getDatabase(this)
        appDao = database.appDao()

        // Initialize Views
        initializeViews()

        // Set up biometric authentication toggle
        fingerprintToggle.isChecked = prefs.getBoolean("fingerprint_enabled", false)
        setupDelayDropdown(prefs)

        fingerprintToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("fingerprint_enabled", isChecked).apply()
        }

        // Set up dynamic theme toggle
        dynamicThemeToggle.isChecked = prefs.getBoolean("dynamic_theming", false)
        dynamicThemeToggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dynamic_theming", isChecked).apply()
            restartActivity()
        }

        // Set up theme change button
        changeThemeButton.setOnClickListener {
            Toast.makeText(this, "Show Color Picker Dialog", Toast.LENGTH_SHORT).show()
        }

        // Handle delete button click
        deleteTableDataButton.setOnClickListener {
            confirmAndDeleteTableData()
        }

        // Setup new buttons in About & Help section
        setupAboutAndHelpButtons()

        // Load table names into dropdown
        loadTableNames()

        // Set app version info
        setAppVersionInfo()
    }

    private fun initializeViews() {
        tableDeleteDropdown = findViewById(R.id.table_delete_dropdown)
        deleteTableDataButton = findViewById(R.id.delete_table_data_button)
        fingerprintToggle = findViewById(R.id.fingerprint_toggle)
        dynamicThemeToggle = findViewById(R.id.dynamic_theme_toggle)
        fingerprintDelayDropdown = findViewById(R.id.fingerprint_delay_dropdown)
        changeThemeButton = findViewById(R.id.change_theme_button)

        // New UI components in updated layout
        appInfoButton = findViewById(R.id.app_info_button)
        helpButton = findViewById(R.id.help_button)
        aboutDeveloperButton = findViewById(R.id.about_developer_button)
        appVersionText = findViewById(R.id.app_version_text)
    }

    override fun onResume() {
        super.onResume()

        val app = application as MyApplication
        if (app.isReopenFromBackground()) {
            app.showBiometricPromptIfNeeded(this) {
                // This is called if authentication is cancelled or fails
                // You might want to finish() the activity in some cases
                finish()
            }
        }
        app.isAppInBackground = false
    }

    override fun onPause() {
        super.onPause()
        val app = application as MyApplication
        app.isAppInBackground = true
        app.markPausedTime()
    }

    private fun setupDelayDropdown(prefs: android.content.SharedPreferences) {
        val delayOptions = (0..30 step 5).map { if (it == 0) "Every time" else "$it minutes" }
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, delayOptions)
        fingerprintDelayDropdown.setAdapter(adapter)

        val savedIndex = prefs.getInt("fingerprint_delay_index", 0)
        fingerprintDelayDropdown.setText(delayOptions[savedIndex], false)

        fingerprintDelayDropdown.setOnItemClickListener { _, _, position, _ ->
            prefs.edit()
                .putInt("fingerprint_delay_index", position)
                .putInt(
                    "fingerprint_delay_value",
                    if (position == 0) 0 else position * 5
                ).apply()
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
            .setMessage("DBST (Dollar Buy Sell Transactions) is a financial tracking app " +
                    "designed to help you manage your personal finances, track income, " +
                    "expenses, and currency exchanges.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("Help & Support")
            .setMessage("For assistance with using DBST, please contact the developer " +
                    "or check the documentation in the About Developer section.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDeveloperInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle("About Developer")
            .setMessage("Developed by Gorun Jinian\n" +
                    "Haigazian University\n" +
                    "Spring Semester 2024-2025\n" +
                    "Beirut, Lebanon")
            .setPositiveButton("OK", null)
            .show()
    }


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