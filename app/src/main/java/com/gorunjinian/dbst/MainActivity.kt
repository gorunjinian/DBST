package com.gorunjinian.dbst

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.NavController
import com.google.android.material.color.DynamicColors
import android.util.TypedValue

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Apply Material 3 Dynamic Colors
        DynamicColors.applyToActivityIfAvailable(this)

        // Initialize Views
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        toolbar = findViewById(R.id.toolbar)

        // Set Toolbar
        setSupportActionBar(toolbar)
        toolbar.inflateMenu(R.menu.toolbar_menu) // Inflate the menu

        // Fix Status Bar and Navigation Bar Colors Dynamically
        val window: Window = this.window
        window.statusBarColor = getColorFromAttr(com.google.android.material.R.attr.colorPrimary) // Matches toolbar
        window.navigationBarColor = getColorFromAttr(com.google.android.material.R.attr.colorSurface) // Matches background

        // Adjust Status Bar Icons for Light & Dark Mode
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode()

        // Get NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        // Configure AppBarConfiguration
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.entryFragment, R.id.validityFragment, R.id.tetherFragment, R.id.infoFragment)
        )

        // Setup navigation with Toolbar, Drawer, and Bottom Nav
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(bottomNavigationView, navController)

        // Listen for navigation changes to update toolbar title dynamically
        navController.addOnDestinationChangedListener { _, destination, _ ->
            toolbar.title = when (destination.id) {
                R.id.entryFragment -> "Entry"
                R.id.validityFragment -> "Validity"
                R.id.tetherFragment -> "USDT"
                R.id.infoFragment -> "Info"
                R.id.databasesFragment -> "Databases"
                R.id.yearlyViewFragment -> "Yearly View"
                R.id.exportDataFragment -> "Export Data"
                else -> getString(R.string.app_name) // Default title
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure toolbar title updates correctly when returning from SettingsActivity
        val currentDestination = navController.currentDestination
        if (currentDestination != null) {
            toolbar.title = when (currentDestination.id) {
                R.id.entryFragment -> "Entry"
                R.id.validityFragment -> "Validity"
                R.id.tetherFragment -> "USDT"
                R.id.infoFragment -> "Info"
                R.id.databasesFragment -> "Databases"
                R.id.yearlyViewFragment -> "Yearly View"
                R.id.exportDataFragment -> "Export Data"
                else -> getString(R.string.app_name) // Default title
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        // Force menu icons to show in overflow menu
        if (menu is androidx.appcompat.view.menu.MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_databases -> {
                navController.navigate(R.id.databasesFragment)
                true
            }
            R.id.nav_yearly_view -> {
                navController.navigate(R.id.yearlyViewFragment)
                true
            }
            R.id.nav_export_data -> {
                navController.navigate(R.id.exportDataFragment)
                true
            }
            R.id.nav_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Helper function to check if dark mode is active
    private fun isDarkMode(): Boolean {
        return resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun getColorFromAttr(attr: Int): Int {
        val typedValue = TypedValue()
        val theme = theme
        if (theme.resolveAttribute(attr, typedValue, true)) {
            return typedValue.data
        }
        return getColor(android.R.color.black) // Fallback color (optional)
    }
}
