package com.gorunjinian.dbst.fabcomponents

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.adapters.PopupPagerAdapter
import com.gorunjinian.dbst.data.AppDatabase
import com.gorunjinian.dbst.data.AppRepository

/**
 * Main manager for the floating action button and its popup
 * This class now delegates the page-specific functionality to specialized managers
 */
@SuppressLint("StaticFieldLeak")
object FabManager {

    private lateinit var viewPager: ViewPager2
    private var cashCounterManager: CashCounterManager? = null
    private var assetManagementManager: AssetManagementManager? = null
    private var checklistManager: ChecklistManager? = null

    fun setupFab(fab: FloatingActionButton, activity: FragmentActivity) {
        fab.setOnClickListener {
            showPopup(activity)
        }
    }

    private fun showPopup(context: Context) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.popup_info)

        // Apply rounded background to popup
        val background: Drawable? = ContextCompat.getDrawable(context, R.drawable.rounded_popup)
        dialog.window?.setBackgroundDrawable(background)

        // Ensure popup appears rounded & properly sized
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setDimAmount(0.8f) // Add dim effect to background

        // Create repository
        val database = AppDatabase.getDatabase(context)
        val appDao = database.appDao()
        val repository = AppRepository(appDao)

        // Set up ViewPager
        setupPager(dialog, repository, context)

        // Set up the Save and Close buttons
        val saveButton: Button = dialog.findViewById(R.id.save_popup)
        val closeButton: Button = dialog.findViewById(R.id.close_popup)

        saveButton.setOnClickListener {
            // Save data from all managers
            cashCounterManager?.saveData(repository)
            // We could also save data from other managers if needed

            dialog.dismiss()
            Toast.makeText(context, "Changes saved", Toast.LENGTH_SHORT).show()
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Adjust popup window size
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun setupPager(dialog: Dialog, repository: AppRepository, context: Context) {
        // Initialize ViewPager2
        viewPager = dialog.findViewById(R.id.view_pager)

        // Create page layouts
        val layouts = listOf(
            R.layout.cash_counter_page,
            R.layout.given_values_page,
            R.layout.collecting_page
        )

        // Setup adapter
        val pagerAdapter = PopupPagerAdapter(layouts)
        viewPager.adapter = pagerAdapter

        // Setup page change listener to initialize appropriate page content
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> initCashCounterPage(repository)
                    1 -> initAssetManagementPage()
                    2 -> initCollectingPage(context)
                }
            }
        })

        // Initialize the first page
        initCashCounterPage(repository)
    }

    private fun initCashCounterPage(repository: AppRepository) {
        val cashCounterView = findViewPagerChildAt(0) ?: return

        // Create and initialize the CashCounterManager if not already created
        if (cashCounterManager == null) {
            cashCounterManager = CashCounterManager(cashCounterView)
        }

        // Initialize with the repository
        cashCounterManager?.initialize(repository)
    }

    private fun initAssetManagementPage() {
        val assetManagementView = findViewPagerChildAt(1) ?: return

        // Create and initialize the AssetManagementManager if not already created
        if (assetManagementManager == null) {
            assetManagementManager = AssetManagementManager(assetManagementView)
        }

        // Initialize the manager
        assetManagementManager?.initialize()
    }

    private fun initCollectingPage(context: Context) {
        val collectingPageView = findViewPagerChildAt(2) ?: return

        // Create and initialize the ChecklistManager if not already created
        if (checklistManager == null) {
            checklistManager = ChecklistManager(context, collectingPageView)
        } else {
            // If manager exists, ensure it reloads data
            checklistManager?.loadItems() // Make loadItems public for this
        }
    }

    // Helper method to find child views inside ViewPager2
    private fun findViewPagerChildAt(position: Int): View? {
        if (position < 0 || !::viewPager.isInitialized) return null

        // Get the RecyclerView inside ViewPager2
        val recyclerView = viewPager.getChildAt(0) as? RecyclerView ?: return null

        // Find the ViewHolder for the specified position
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) ?: return null

        // Return the itemView
        return viewHolder.itemView
    }
}