package com.gorunjinian.dbst.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.MainPagerAdapter
import com.gorunjinian.dbst.fragments.DatabasesFragment
import com.gorunjinian.dbst.fragments.ExportDataFragment
import com.gorunjinian.dbst.fragments.YearlyViewFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var viewPager: ViewPager2
    private lateinit var fullScreenContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.Base_Theme_DBST)

        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            // If your bar is bright and you want dark icons:
            isAppearanceLightStatusBars = true

            // Or if your bar is dark and you want light icons:
            //isAppearanceLightStatusBars = true
        }



        viewPager = findViewById(R.id.viewPager)
        fullScreenContainer = findViewById(R.id.full_screen_container)

        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = true

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.entryFragment -> viewPager.currentItem = 0
                R.id.validityFragment -> viewPager.currentItem = 1
                R.id.tetherFragment -> viewPager.currentItem = 2
                R.id.infoFragment -> viewPager.currentItem = 3
            }
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNavigationView.menu.getItem(position).isChecked = true
                supportActionBar?.title = getToolbarTitle(position) // Ensure title updates correctly
            }
        })


    }


    override fun onResume() {
        super.onResume()
        supportActionBar?.title = getToolbarTitle(viewPager.currentItem) // Fix toolbar title

        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES)

        // If it's night mode, we want light icons; if it's day mode, we want dark icons.
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isNightMode

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

    private fun navigateToFullScreenFragment(fragment: androidx.fragment.app.Fragment, title: String) {
        bottomNavigationView.visibility = View.GONE
        viewPager.visibility = View.GONE
        fullScreenContainer.visibility = View.VISIBLE
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.beginTransaction()
            .replace(R.id.full_screen_container, fragment)
            .addToBackStack(null)
            .commit()

        supportActionBar?.title = title
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            bottomNavigationView.visibility = View.VISIBLE
            viewPager.visibility = View.VISIBLE
            fullScreenContainer.visibility = View.GONE
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.title = getToolbarTitle(viewPager.currentItem)
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
}
