package com.gorunjinian.dbst.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.NavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors
import com.gorunjinian.dbst.R

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
        toolbar.inflateMenu(R.menu.toolbar_menu)

        // Apply Status Bar and Navigation Bar Colors
        applySystemBarColors()

        // Setup Navigation Components
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.entryFragment, R.id.validityFragment, R.id.tetherFragment, R.id.infoFragment)
        )

        // Fix Navigation Action Bar
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(bottomNavigationView, navController)

        // Update toolbar title dynamically when navigating
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

            // Fragments that should hide bottom navigation and show back button
            val fullScreenFragments = setOf(
                R.id.databasesFragment,
                R.id.yearlyViewFragment,
                R.id.exportDataFragment
            )

            if (fullScreenFragments.contains(destination.id)) {
                bottomNav.visibility = View.GONE // Hide Bottom Navigation
                supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show Back Button
            } else {
                bottomNav.visibility = View.VISIBLE // Show Bottom Navigation
                supportActionBar?.setDisplayHomeAsUpEnabled(false) // Hide Back Button for Main Screens
            }

            // Set toolbar title dynamically
            toolbar.title = getToolbarTitle(destination.id)
        }

    }

    override fun onResume() {
        super.onResume()
        toolbar.title = getToolbarTitle(navController.currentDestination?.id)
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (navController.currentDestination?.id in setOf(
                R.id.databasesFragment,
                R.id.yearlyViewFragment,
                R.id.exportDataFragment
            )) {
            navController.popBackStack() // Go back when back button is clicked
            true
        } else {
            super.onSupportNavigateUp()
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        if (menu is androidx.appcompat.view.menu.MenuBuilder) {
            menu.setOptionalIconsVisible(true) // Force icons to be visible in overflow
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
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getToolbarTitle(destinationId: Int?): String {
        return when (destinationId) {
            R.id.entryFragment -> "Entry"
            R.id.validityFragment -> "Validity"
            R.id.tetherFragment -> "USDT"
            R.id.infoFragment -> "Info"
            R.id.databasesFragment -> "Databases"
            R.id.yearlyViewFragment -> "Yearly View"
            R.id.exportDataFragment -> "Export Data"
            else -> getString(R.string.app_name)
        }
    }

    private fun applySystemBarColors() {
        val window: Window = this.window

        // Set status bar color to match toolbar
        window.statusBarColor = getColor(R.color.primary)

        // Set navigation bar color correctly based on theme
        val navBarColor = if (isDarkMode()) R.color.bottom_nav_dark else R.color.bottom_nav_light
        window.navigationBarColor = getColor(navBarColor)

        // Apply correct bottom navigation color dynamically
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setBackgroundColor(getColor(navBarColor))

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)

        // Ensure light/dark icons based on theme
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode()
        windowInsetsController.isAppearanceLightNavigationBars = !isDarkMode()
    }



    private fun isDarkMode(): Boolean {
        return resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun getColorFromAttr(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}