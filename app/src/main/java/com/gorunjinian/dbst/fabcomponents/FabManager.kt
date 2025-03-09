package com.gorunjinian.dbst.fabcomponents

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
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
 */
@SuppressLint("StaticFieldLeak")
object FabManager {
    private const val TAG = "FabManager"

    // ViewPager and managers
    private var viewPager: ViewPager2? = null
    private var cashCounterManager: CashCounterManager? = null
    private var givenValuesManager: GivenValuesManager? = null
    private var checklistManager: ChecklistManager? = null

    // Track current page
    private var currentPagePosition = 0

    // Repository instance
    private lateinit var repository: AppRepository

    // Dialog reference
    private var popupDialog: Dialog? = null

    fun setupFab(fab: FloatingActionButton, activity: FragmentActivity) {
        // Create and cache repository
        val database = AppDatabase.getDatabase(activity)
        val appDao = database.appDao()
        repository = AppRepository(appDao)

        fab.setOnClickListener {
            showPopup(activity)
        }
    }

    private fun showPopup(context: Context) {
        Log.d(TAG, "Opening popup")

        val dialog = Dialog(context)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.popup_info)

        // Store reference
        popupDialog = dialog

        // Apply rounded background
        val background: Drawable? = ContextCompat.getDrawable(context, R.drawable.rounded_popup)
        dialog.window?.setBackgroundDrawable(background)

        // Ensure popup appears properly sized
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setDimAmount(0.8f)

        // Set up ViewPager
        setupPager(dialog, context)

        // Set up buttons
        val saveButton: Button = dialog.findViewById(R.id.save_popup)
        val closeButton: Button = dialog.findViewById(R.id.close_popup)

        saveButton.setOnClickListener {
            Log.d(TAG, "Save button clicked for page $currentPagePosition")

            // Save data based on current page
            when (currentPagePosition) {
                0 -> {
                    Log.d(TAG, "Saving cash counter data")
                    cashCounterManager?.saveData(repository)
                }
                1 -> {
                    Log.d(TAG, "Saving given values data")
                    givenValuesManager?.saveData()
                }
                2 -> {
                    Log.d(TAG, "Saving checklist data")
                    checklistManager?.saveData()
                }
            }

            dialog.dismiss()
            popupDialog = null

            Toast.makeText(context, "Changes saved", Toast.LENGTH_SHORT).show()
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
            popupDialog = null
        }

        dialog.show()
    }

    private fun setupPager(dialog: Dialog, context: Context) {
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
        viewPager?.adapter = pagerAdapter

        // Setup page change listener
        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // Update current page position
                currentPagePosition = position
                Log.d(TAG, "Page selected: $position")

                // Initialize the page
                when (position) {
                    0 -> {
                        Log.d(TAG, "Initializing cash counter page")
                        initCashCounterPage(context)
                    }
                    1 -> {
                        Log.d(TAG, "Initializing given values page")
                        initGivenValuesPage()
                    }
                    2 -> {
                        Log.d(TAG, "Initializing checklist page")
                        initCollectingPage(context)
                    }
                }

                // Adjust dialog height
                updateDialogHeight()
            }
        })

        // Initialize first page
        initCashCounterPage(context)
    }

    private fun initCashCounterPage(context: Context) {
        val cashCounterView = findViewPagerChildAt(0)
        if (cashCounterView == null) {
            Log.e(TAG, "Cash counter view not found")
            return
        }

        try {
            // Initialize the CashCounterManager
            if (cashCounterManager == null) {
                Log.d(TAG, "Creating new CashCounterManager")
                cashCounterManager = CashCounterManager(cashCounterView)
                cashCounterManager?.initialize(repository)
            } else {
                Log.d(TAG, "Refreshing existing CashCounterManager")
                // Use the new refreshView method
                cashCounterManager?.refreshView(cashCounterView)
                // Then load data
                cashCounterManager?.loadData(repository)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing cash counter page: ${e.message}", e)
        }
    }

    private fun initGivenValuesPage() {
        Log.e(TAG, "==================================================")
        Log.e(TAG, "INIT GIVEN VALUES PAGE CALLED")
        Log.e(TAG, "==================================================")

        val givenValuesView = findViewPagerChildAt(1)
        if (givenValuesView == null) {
            Log.e(TAG, "Given values view not found")
            return
        }

        Log.e(TAG, "Given values view found, proceeding...")

        try {
            // Initialize the GivenValuesManager
            if (givenValuesManager == null) {
                Log.e(TAG, "Creating new GivenValuesManager")
                givenValuesManager = GivenValuesManager(givenValuesView)
            } else {
                Log.e(TAG, "Refreshing existing GivenValuesManager")
                givenValuesManager?.refreshView(givenValuesView)
            }

            // Initialize with repository
            Log.e(TAG, "Initializing GivenValuesManager with repository")
            givenValuesManager?.initialize(repository)
            Log.e(TAG, "GivenValuesManager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing given values page: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun initCollectingPage(context: Context) {
        val collectingPageView = findViewPagerChildAt(2)
        if (collectingPageView == null) {
            Log.e(TAG, "Collecting page view not found")
            return
        }

        try {
            // Initialize the ChecklistManager
            if (checklistManager == null) {
                Log.d(TAG, "Creating new ChecklistManager")
                checklistManager = ChecklistManager(context, collectingPageView, repository)

                // No need to explicitly call loadItems() as the init block should handle it
            } else {
                Log.d(TAG, "Refreshing existing ChecklistManager")
                checklistManager?.refreshView(collectingPageView)

                // Explicitly call loadItems to ensure data is refreshed
                checklistManager?.loadItems()
            }

            // Update dialog height based on checked items
            updateDialogHeight()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing checklist page: ${e.message}", e)
        }
    }

    private fun updateDialogHeight() {
        val dialog = popupDialog ?: return

        // Expand to full height unless on checklist page with no checked items
        val shouldCollapse = currentPagePosition == 2 &&
                checklistManager?.hasCheckedItems() == false

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            if (shouldCollapse) ViewGroup.LayoutParams.WRAP_CONTENT
            else ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Helper method to find child views
    private fun findViewPagerChildAt(position: Int): View? {
        val viewPager = this.viewPager ?: return null

        try {
            // Get the RecyclerView inside ViewPager2
            val recyclerView = viewPager.getChildAt(0) as? RecyclerView
            if (recyclerView == null) {
                Log.e(TAG, "RecyclerView not found in ViewPager2")
                return null
            }

            // Find the ViewHolder for the position
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
            if (viewHolder == null) {
                Log.e(TAG, "ViewHolder not found for position $position")
                return null
            }

            return viewHolder.itemView
        } catch (e: Exception) {
            Log.e(TAG, "Error finding view at position $position: ${e.message}", e)
            return null
        }
    }
}