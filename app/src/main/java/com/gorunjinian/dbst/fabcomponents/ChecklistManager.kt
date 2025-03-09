package com.gorunjinian.dbst.fabcomponents

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.adapters.ChecklistAdapter
import com.gorunjinian.dbst.data.AppRepository
import com.gorunjinian.dbst.data.ChecklistItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manager for the Checklist page in the FAB popup
 */
class ChecklistManager(
    private val context: Context,
    rootView: View,
    private val repository: AppRepository
) {
    private val TAG = "ChecklistManager"

    // Current rootView
    private var currentRootView: View = rootView

    // UI components
    private lateinit var newItemText: EditText
    private lateinit var addItemButton: ImageView
    private lateinit var uncheckedItemsList: RecyclerView
    private lateinit var checkedItemsList: RecyclerView
    private lateinit var checkedItemsHeader: LinearLayout
    private lateinit var expandArrow: ImageView
    private lateinit var checkedItemsCount: TextView

    // Touch helper
    private var itemTouchHelper: ItemTouchHelper? = null

    // Data
    private val uncheckedItems = mutableListOf<ChecklistItem>()
    private val checkedItems = mutableListOf<ChecklistItem>()

    // Adapters
    private var uncheckedAdapter: ChecklistAdapter? = null
    private var checkedAdapter: ChecklistAdapter? = null

    // State
    private var isCheckedItemsExpanded = false
    private var initialized = false

    init {
        initializeViews()
    }

    /**
     * Update the rootView reference
     */
    fun refreshView(rootView: View) {
        currentRootView = rootView
        initializeViews()
    }

    private fun initializeViews() {
        Log.d(TAG, "Initializing views")

        try {
            // Find views
            newItemText = currentRootView.findViewById(R.id.new_item_text)
            addItemButton = currentRootView.findViewById(R.id.add_item_button)
            uncheckedItemsList = currentRootView.findViewById(R.id.unchecked_items_list)
            checkedItemsList = currentRootView.findViewById(R.id.checked_items_list)
            checkedItemsHeader = currentRootView.findViewById(R.id.checked_items_header)
            expandArrow = currentRootView.findViewById(R.id.expand_arrow)
            checkedItemsCount = currentRootView.findViewById(R.id.checked_items_count)

            // Set up RecyclerViews and adapters
            setupRecyclerViews()

            // Set up listeners
            setupListeners()

            initialized = true

            // Don't load items here - let caller decide when to load
            // We removed the loadItems() call that was here
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
        }
    }

    private fun setupRecyclerViews() {
        Log.d(TAG, "Setting up RecyclerViews")

        try {
            // Only create adapters if they don't exist yet
            if (uncheckedAdapter == null) {
                Log.d(TAG, "Creating new unchecked adapter")
                uncheckedAdapter = ChecklistAdapter(
                    uncheckedItems,
                    onItemChecked = { item -> toggleItemChecked(item) },
                    onItemDeleted = { item -> deleteItem(item) }
                ).apply {
                    // Set drag callback
                    onStartDrag = { viewHolder -> itemTouchHelper?.startDrag(viewHolder) }
                }
            }

            if (checkedAdapter == null) {
                Log.d(TAG, "Creating new checked adapter")
                checkedAdapter = ChecklistAdapter(
                    checkedItems,
                    onItemChecked = { item -> toggleItemChecked(item) },
                    onItemDeleted = { item -> deleteItem(item) }
                )
            }

            // Always set the adapters to the new RecyclerViews
            uncheckedItemsList.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = uncheckedAdapter
                setHasFixedSize(true)
            }

            // Set up checked items list
            checkedItemsList.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = checkedAdapter
                setHasFixedSize(true)
            }

            // Create and attach ItemTouchHelper if needed
            if (itemTouchHelper == null) {
                val callback = object : ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    0
                ) {
                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean {
                        val fromPosition = viewHolder.bindingAdapterPosition
                        val toPosition = target.bindingAdapterPosition

                        // Check bounds
                        if (fromPosition < 0 || toPosition < 0 ||
                            fromPosition >= uncheckedItems.size ||
                            toPosition >= uncheckedItems.size) {
                            return false
                        }

                        // Move item in list
                        val item = uncheckedItems[fromPosition]
                        uncheckedItems.removeAt(fromPosition)
                        uncheckedItems.add(toPosition, item)
                        uncheckedAdapter?.notifyItemMoved(fromPosition, toPosition)

                        // Update positions in database
                        updateItemPositions()
                        return true
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        // Not using swipe
                    }
                }

                itemTouchHelper = ItemTouchHelper(callback)
            }

            // Always reattach the touch helper to the new RecyclerView
            itemTouchHelper?.attachToRecyclerView(uncheckedItemsList)

            // Initially hide checked items section
            checkedItemsList.visibility = View.GONE

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerViews: ${e.message}", e)
        }
    }

    private fun setupListeners() {
        Log.d(TAG, "Setting up listeners")

        try {
            // Add item button
            addItemButton.setOnClickListener {
                addNewItem()
            }

            // Enter key
            newItemText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addNewItem()
                    return@setOnEditorActionListener true
                }
                false
            }

            // Checked items header
            checkedItemsHeader.setOnClickListener {
                toggleCheckedItemsVisibility()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up listeners: ${e.message}", e)
        }
    }

    private fun addNewItem() {
        val text = newItemText.text.toString().trim()

        if (text.isEmpty()) {
            return
        }

        try {
            Log.d(TAG, "Adding new item: \"$text\"")

            // Create new item
            val newItem = ChecklistItem(
                text = text,
                isChecked = false,
                position = uncheckedItems.size
            )

            // Save to database
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val id = repository.insertChecklistItem(newItem)
                    Log.d(TAG, "Item saved with ID: $id")

                    // Reload items
                    loadItems()
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving new item: ${e.message}", e)
                }
            }

            // Clear input and hide keyboard
            newItemText.text.clear()

            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(newItemText.windowToken, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding new item: ${e.message}", e)
        }
    }

    private fun toggleItemChecked(item: ChecklistItem) {
        Log.d(TAG, "Toggling item checked state: $item")

        try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Update item in database
                    val updatedItem = item.copy(isChecked = !item.isChecked)
                    repository.updateChecklistItem(updatedItem)
                    Log.d(TAG, "Item updated: $updatedItem")

                    // Reload items
                    loadItems()
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating item: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling item checked state: ${e.message}", e)
        }
    }

    private fun deleteItem(item: ChecklistItem) {
        Log.d(TAG, "Deleting item: $item")

        try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Delete from database
                    repository.deleteChecklistItem(item.id)
                    Log.d(TAG, "Item deleted")

                    // Reload items
                    loadItems()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting item: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting item: ${e.message}", e)
        }
    }

    private fun toggleCheckedItemsVisibility() {
        Log.d(TAG, "Toggling checked items visibility")

        try {
            // Only toggle if there are checked items
            if (checkedItems.isEmpty()) {
                isCheckedItemsExpanded = false
                checkedItemsList.visibility = View.GONE
                expandArrow.rotation = 0f
                return
            }

            isCheckedItemsExpanded = !isCheckedItemsExpanded

            if (isCheckedItemsExpanded) {
                checkedItemsList.visibility = View.VISIBLE
                expandArrow.animate().rotation(180f).setDuration(200).start()
            } else {
                checkedItemsList.visibility = View.GONE
                expandArrow.animate().rotation(0f).setDuration(200).start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling checked items visibility: ${e.message}", e)
        }
    }

    private fun updateCheckedItemsVisibility() {
        Log.d(TAG, "Updating checked items visibility")

        try {
            // Hide checked items section if empty
            if (checkedItems.isEmpty()) {
                checkedItemsHeader.visibility = View.GONE
                checkedItemsList.visibility = View.GONE
                isCheckedItemsExpanded = false
                Log.d(TAG, "No checked items, hiding section")
            } else {
                checkedItemsHeader.visibility = View.VISIBLE
                checkedItemsList.visibility = if (isCheckedItemsExpanded) View.VISIBLE else View.GONE
                expandArrow.rotation = if (isCheckedItemsExpanded) 180f else 0f
                Log.d(TAG, "Checked items available, showing section")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating checked items visibility: ${e.message}", e)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCheckedItemsCount() {
        try {
            checkedItemsCount.text = "${checkedItems.size} Checked items"
        } catch (e: Exception) {
            Log.e(TAG, "Error updating checked items count: ${e.message}", e)
        }
    }

    private fun updateItemPositions() {
        Log.d(TAG, "Updating item positions")

        try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Update positions
                    uncheckedItems.forEachIndexed { index, item ->
                        repository.updateChecklistItemPosition(item.id, index)
                    }
                    Log.d(TAG, "Item positions updated")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating item positions: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating item positions: ${e.message}", e)
        }
    }

    fun hasCheckedItems(): Boolean {
        return checkedItems.isNotEmpty()
    }

    fun saveData(repository: AppRepository = this.repository) {
        Log.d(TAG, "Saving checklist data")

        // Checklist items are saved immediately on each action
        // This method exists for consistency with other managers
    }

    @SuppressLint("NotifyDataSetChanged")
    fun loadItems() {
        if (!initialized) {
            Log.e(TAG, "Cannot load items: view not initialized")
            return
        }

        Log.d(TAG, "Loading checklist items")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get items from database
                val fetchedUncheckedItems = repository.getUncheckedItems()
                val fetchedCheckedItems = repository.getCheckedItems()

                Log.d(TAG, "Found ${fetchedUncheckedItems.size} unchecked items and ${fetchedCheckedItems.size} checked items")

                withContext(Dispatchers.Main) {
                    try {
                        // Update unchecked items
                        uncheckedItems.clear()
                        uncheckedItems.addAll(fetchedUncheckedItems)
                        uncheckedAdapter?.notifyDataSetChanged()

                        // Update checked items
                        checkedItems.clear()
                        checkedItems.addAll(fetchedCheckedItems)
                        checkedAdapter?.notifyDataSetChanged()

                        // Update UI
                        updateCheckedItemsCount()
                        updateCheckedItemsVisibility()

                        Log.d(TAG, "Checklist items updated in UI")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating UI with checklist items: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading checklist items: ${e.message}", e)
            }
        }
    }
}