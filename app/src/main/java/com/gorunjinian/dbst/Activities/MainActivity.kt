package com.gorunjinian.dbst.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.MainPagerAdapter
import com.gorunjinian.dbst.fragments.DatabasesFragment
import com.gorunjinian.dbst.fragments.ExportDataFragment
import com.gorunjinian.dbst.fragments.YearlyViewFragment
import com.gorunjinian.dbst.FabManager

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var viewPager: ViewPager2
    private lateinit var fullScreenContainer: FrameLayout
    private lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.Base_Theme_DBST)
        setContentView(R.layout.activity_main)

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        toolbar = findViewById(R.id.toolbar)
        viewPager = findViewById(R.id.viewPager)
        fullScreenContainer = findViewById(R.id.full_screen_container)
        fab = findViewById(R.id.fab_popup)

        setSupportActionBar(toolbar)

        // Set up FAB using the FabManager
        FabManager.setupFab(fab, viewPager, this)

        // Adjust system UI appearance (status bar icons)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        // Setup ViewPager and BottomNavigationView
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
                supportActionBar?.title = getToolbarTitle(position)
            }
        })

    }

    private fun showPopup() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.popup_info)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    override fun onResume() {
        super.onResume()

        // Ensure viewPager is initialized before accessing it
        if (::viewPager.isInitialized) {
            supportActionBar?.title = getToolbarTitle(viewPager.currentItem)
        }

        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES)

        // Adjust status bar icons based on theme
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

    private fun navigateToFullScreenFragment(fragment: Fragment, title: String) {
        Log.d("FragmentDebug", "Navigating to: $title")

        // Hide bottom navigation and ViewPager
        bottomNavigationView.visibility = View.GONE
        viewPager.visibility = View.GONE
        fullScreenContainer.visibility = View.VISIBLE
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.beginTransaction()
            .replace(R.id.full_screen_container, fragment)
            .addToBackStack(title) // ✅ Ensure it adds to the back stack properly
            .setReorderingAllowed(true)
            .commit()

        supportActionBar?.title = title
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()

            // ✅ Restore UI elements when returning to the main screen
            bottomNavigationView.visibility = View.VISIBLE
            viewPager.visibility = View.VISIBLE
            fullScreenContainer.visibility = View.GONE
            supportActionBar?.setDisplayHomeAsUpEnabled(false)

            // ✅ Restore toolbar title
            supportActionBar?.title = getToolbarTitle(viewPager.currentItem)

        } else {
            super.onBackPressed() // Exit app only if there are no fragments in back stack
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
