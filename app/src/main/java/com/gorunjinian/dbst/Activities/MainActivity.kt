package com.gorunjinian.dbst.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.gorunjinian.dbst.FabManager
import com.gorunjinian.dbst.MainPagerAdapter
import com.gorunjinian.dbst.MyApplication
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.ThemeManager
import com.gorunjinian.dbst.fragments.DatabasesFragment
import com.gorunjinian.dbst.fragments.ExportDataFragment
import com.gorunjinian.dbst.fragments.YearlyViewFragment
import androidx.core.view.size

@SuppressLint("RestrictedApi")
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var viewPager: ViewPager2
    private lateinit var fullScreenContainer: FrameLayout
    private lateinit var fab: FloatingActionButton
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply dynamic colors if enabled before setting content view
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val dynamicEnabled = prefs.getBoolean("dynamic_theming", false)

        // Only set static theme if dynamic colors are disabled
        if (!dynamicEnabled) {
            setTheme(R.style.Theme_DBST)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        topAppBar = findViewById(R.id.topAppBar)
        viewPager = findViewById(R.id.viewPager)
        fullScreenContainer = findViewById(R.id.full_screen_container)
        fab = findViewById(R.id.fab_popup)

        // Set up the TopAppBar
        setSupportActionBar(topAppBar)

        // Update system bars and UI elements for current theme
        updateSystemUi()

        // Set up FAB using the FabManager
        FabManager.setupFab(fab, this)

        // Setup ViewPager and BottomNavigationView
        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = true

        // Update the validity tab visibility based on preferences
        setupBottomNavigation()

        // Store callback reference to properly unregister later
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNavigationView.menu[position].isChecked = true
                topAppBar.title = getToolbarTitle(position)
            }
        }.also {
            viewPager.registerOnPageChangeCallback(it)
        }

        // Handle back press with the new API (replacing deprecated onBackPressed)
        setupBackNavigation()
    }

    private fun setupBottomNavigation() {
        // Configure navigation item listener
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.entryFragment -> viewPager.currentItem = 0
                R.id.validityFragment -> viewPager.currentItem = 1
                R.id.tetherFragment -> viewPager.currentItem = 2
                R.id.infoFragment -> viewPager.currentItem = 3
            }
            true
        }

        // Handle visibility of the Validity tab
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val hideValidityTab = prefs.getBoolean("turn_off_validity_tab", false)
        if (hideValidityTab) {
            bottomNavigationView.menu.findItem(R.id.validityFragment)?.isVisible = false
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()

                    // Restore UI elements when returning to the main screen
                    bottomNavigationView.visibility = View.VISIBLE
                    viewPager.visibility = View.VISIBLE
                    fullScreenContainer.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)

                    // Restore toolbar title
                    if (::viewPager.isInitialized) {
                        topAppBar.title = getToolbarTitle(viewPager.currentItem)
                    }

                    // Update system bars when returning from fragment
                    updateSystemUi()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed() // Let system handle app exit
                }
            }
        })
    }

    private fun updateSystemUi() {
        // This method replaces updateSystemBars and updateFabColor with a more
        // comprehensive approach that works with our ThemeManager

        // Get current theme information
        val isNightMode = resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        // Get the primary color from the current theme
        val typedValue = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data

        // Configure system bars
        window.statusBarColor = primaryColor
        window.navigationBarColor = primaryColor

        // Configure system bar icons (light in dark theme, dark in light theme)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isNightMode
        windowInsetsController.isAppearanceLightNavigationBars = !isNightMode

        // Update FAB color to match the toolbar/primary color
        fab.backgroundTintList = ColorStateList.valueOf(primaryColor)
    }

    override fun onResume() {
        super.onResume()

        val app = application as MyApplication
        if (app.isAppInBackground) {
            app.showBiometricPromptIfNeeded(this) {
                finish()
            }
        }

        // Check if recreation is needed due to theme changes
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("needs_recreation", false)) {
            // Clear the flag BEFORE recreating to prevent loops
            prefs.edit {
                putBoolean("needs_recreation", false)
                apply()
            }

            Log.d("ThemeDebug", "Recreating MainActivity for theme changes")
            recreate()
            return // Exit early to avoid updating UI twice
        }

        // Always update UI when resuming
        updateSystemUi()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Update system UI on configuration changes (e.g., dark mode toggle)
        updateSystemUi()
    }

    override fun onDestroy() {
        // Remove the callback to prevent memory leaks
        pageChangeCallback?.let { viewPager.unregisterOnPageChangeCallback(it) }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        if (menu is androidx.appcompat.view.menu.MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        // Apply color to all menu items' icons - always use white for better visibility
        val iconColor = ContextCompat.getColor(this, android.R.color.white)
        for (i in 0 until menu.size) {
            val item = menu[i]
            item.icon?.setTint(iconColor)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.nav_databases -> {
                navigateToFullScreenFragment(DatabasesFragment(), "Databases")
                true
            }
            R.id.nav_yearly_view -> {
                navigateToFullScreenFragment(YearlyViewFragment(), "Yearly View")
                true
            }
            R.id.nav_export_data -> {
                navigateToFullScreenFragment(ExportDataFragment(), "Export Data")
                true
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun navigateToFullScreenFragment(fragment: Fragment, title: String) {
        Log.d("FragmentDebug", "Navigating to: $title")

        // Hide bottom navigation and ViewPager
        bottomNavigationView.visibility = View.GONE
        viewPager.visibility = View.GONE
        fullScreenContainer.visibility = View.VISIBLE
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.beginTransaction()
            .replace(R.id.full_screen_container, fragment)
            .addToBackStack(title) // Ensure it adds to the back stack properly
            .setReorderingAllowed(true)
            .commit()

        topAppBar.title = title
    }

    private fun getToolbarTitle(destinationId: Int?): String {
        return when (destinationId) {
            0, R.id.entryFragment -> "Entry"
            1, R.id.validityFragment -> "Validity"
            2, R.id.tetherFragment -> "USDT"
            3, R.id.infoFragment -> "Dashboard"
            R.id.databasesFragment -> "Databases"
            R.id.yearlyViewFragment -> "Yearly View"
            R.id.exportDataFragment -> "Export Data"
            else -> getString(R.string.app_name)
        }
    }
}