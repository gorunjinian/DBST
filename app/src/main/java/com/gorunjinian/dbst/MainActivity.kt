package com.gorunjinian.dbst

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import androidx.navigation.NavController

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        toolbar = findViewById(R.id.toolbar)

        // Set Toolbar
        setSupportActionBar(toolbar)

        // Get NavController properly
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        // Configure AppBarConfiguration
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.entryFragment, R.id.tetherFragment, R.id.validityFragment, R.id.infoFragment),
            drawerLayout
        )

        // Setup navigation with Toolbar, Drawer, and Bottom Nav
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(bottomNavigationView, navController)
        NavigationUI.setupWithNavController(navigationView, navController)

        // Listen for navigation changes to update toolbar title
        navController.addOnDestinationChangedListener { _, destination, _ ->
            toolbar.title = when (destination.id) {
                R.id.entryFragment -> "Entry"
                R.id.validityFragment -> "Validity"
                R.id.tetherFragment -> "USDT"
                R.id.infoFragment -> "Info"
                R.id.databasesFragment -> "Databases"
                R.id.yearlyViewFragment -> "Yearly View"
                R.id.exportDataFragment -> "Export Data"
                else -> getString(R.string.app_name) // Default title (or use app name)
            }
        }

        // Handle Drawer Menu Clicks
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_databases -> navController.navigate(R.id.databasesFragment)
                R.id.nav_yearly_view -> navController.navigate(R.id.yearlyViewFragment)
                R.id.nav_export_data -> navController.navigate(R.id.exportDataFragment)
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    toolbar.title = "Settings" // Set title manually for Settings
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START) // Close drawer after selection
            true
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

    // Ensure Back Button Closes Drawer If Open
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
