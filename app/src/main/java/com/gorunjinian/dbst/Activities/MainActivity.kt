package com.gorunjinian.dbst.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.MainPagerAdapter
import com.gorunjinian.dbst.fragments.DatabasesFragment
import com.gorunjinian.dbst.fragments.ExportDataFragment
import com.gorunjinian.dbst.fragments.YearlyViewFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DynamicColors.applyToActivityIfAvailable(this)

        // Initialize Views
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        toolbar = findViewById(R.id.toolbar)
        viewPager = findViewById(R.id.viewPager)

        // Set Toolbar
        setSupportActionBar(toolbar)
        toolbar.inflateMenu(R.menu.toolbar_menu)

        // Apply Status Bar and Navigation Bar Colors
        applySystemBarColors()

        // Set up ViewPager2 Adapter
        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter

        // Disable swipe for specific fragments if needed later
        viewPager.isUserInputEnabled = true

        // Synchronize BottomNavigationView item clicks with ViewPager2
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.entryFragment -> viewPager.currentItem = 0
                R.id.validityFragment -> viewPager.currentItem = 1
                R.id.tetherFragment -> viewPager.currentItem = 2
                R.id.infoFragment -> viewPager.currentItem = 3
            }
            true
        }

        // Update BottomNavigationView selection and Toolbar title based on page swipes
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNavigationView.menu.getItem(position).isChecked = true
                toolbar.title = getToolbarTitle(position)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        toolbar.title = getToolbarTitle(viewPager.currentItem)
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        if (menu is androidx.appcompat.view.menu.MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.nav_databases -> {
                navigateToFullScreenFragment(R.id.databasesFragment)
                true
            }
            R.id.nav_yearly_view -> {
                navigateToFullScreenFragment(R.id.yearlyViewFragment)
                true
            }
            R.id.nav_export_data -> {
                navigateToFullScreenFragment(R.id.exportDataFragment)
                true
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun navigateToFullScreenFragment(fragmentId: Int) {
        bottomNavigationView.visibility = View.GONE
        viewPager.visibility = View.GONE
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val fragment = when (fragmentId) {
            R.id.databasesFragment -> DatabasesFragment()
            R.id.yearlyViewFragment -> YearlyViewFragment()
            R.id.exportDataFragment -> ExportDataFragment()
            else -> null
        }

        fragment?.let {
            supportFragmentManager.beginTransaction()
                .replace(R.id.full_screen_container, it)
                .addToBackStack(null)
                .commit()
            toolbar.title = getToolbarTitle(fragmentId)
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            bottomNavigationView.visibility = View.VISIBLE
            viewPager.visibility = View.VISIBLE
            findViewById<FrameLayout>(R.id.full_screen_container).visibility = View.GONE
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            toolbar.title = getToolbarTitle(viewPager.currentItem)
        } else {
            super.onBackPressed()
        }
    }


    private fun getToolbarTitle(destinationId: Int?): String {
        return when (destinationId) {
            0, R.id.entryFragment -> "Entry"
            1, R.id.validityFragment -> "Validity"
            2, R.id.tetherFragment -> "USDT"
            3, R.id.infoFragment -> "Info"
            R.id.databasesFragment -> "Databases"
            R.id.yearlyViewFragment -> "Yearly View"
            R.id.exportDataFragment -> "Export Data"
            else -> getString(R.string.app_name)
        }
    }

    private fun applySystemBarColors() {
        val window: Window = this.window

        window.statusBarColor = getColor(R.color.primary)

        val navBarColor = if (isDarkMode()) R.color.bottom_nav_dark else R.color.bottom_nav_light
        window.navigationBarColor = getColor(navBarColor)

        bottomNavigationView.setBackgroundColor(getColor(navBarColor))

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
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
