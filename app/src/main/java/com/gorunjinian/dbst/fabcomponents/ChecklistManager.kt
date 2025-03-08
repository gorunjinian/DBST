package com.gorunjinian.dbst.fabcomponents

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.data.AppDatabase
import com.gorunjinian.dbst.data.AppRepository
import com.gorunjinian.dbst.data.ChecklistItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manager for the Checklist page in the FAB popup
 * Handles checklist item creation, ordering, checking, and persistence using Room database
 */
class ChecklistManager(private val context: Context, private val rootView: View) {

    // UI components
    private val newItemText: EditText = rootView.findViewById(R.id.new_item_text)
    private val addItemButton: ImageView = rootView.findViewById(R.id.add_item_button)
    private val uncheckedItemsList: RecyclerView = rootView.findViewById(R.id.unchecked_items_list)
    private val checkedItemsList: RecyclerView = rootView.findViewById(R.id.checked_items_list)
    private val checkedItemsHeader: LinearLayout = rootView.findViewById(R.id.checked_items_header)
    private val expandArrow: ImageView = rootView.findViewById(R.id.expand_arrow)
    private val checkedItemsCount: TextView = rootView.findViewById(R.id.checked_items_count)
    private lateinit var itemTouchHelper: ItemTouchHelper

    // Repository for database operations
    private val repository: AppRepository

    // Data
    private val uncheckedItems = mutableListOf<ChecklistItem>()
    private val checkedItems = mutableListOf<ChecklistItem>()

    // Adapters
    private val uncheckedAdapter = ChecklistAdapter(
        uncheckedItems,
        onCheckedChange = { item, isChecked -> handleItemChecked(item, isChecked) }
    )

    private val checkedAdapter = ChecklistAdapter(
        checkedItems,
        onCheckedChange = { item, isChecked -> handleItemChecked(item, isChecked) }
    )

    // State
    private var isCheckedItemsExpanded = false

    init {
        // Initialize repository
        val database = AppDatabase.getDatabase(context)
        repository = AppRepository(database.appDao())

        setupRecyclerViews()
        setupListeners()
        loadItems()
    }

    private fun setupRecyclerViews() {
        // Setup unchecked items RecyclerView
        uncheckedItemsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = uncheckedAdapter
            setHasFixedSize(true)
        }

        // Setup checked items RecyclerView
        checkedItemsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = checkedAdapter
            setHasFixedSize(true)
        }

        // Set up ItemTouchHelper for drag-and-drop reordering
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
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

                // Swap items in the list
                if (fromPosition < uncheckedItems.size && toPosition < uncheckedItems.size) {
                    // Get the moved item
                    val item = uncheckedItems[fromPosition]

                    // Move item in UI list
                    uncheckedItems.removeAt(fromPosition)
                    uncheckedItems.add(toPosition, item)
                    uncheckedAdapter.notifyItemMoved(fromPosition, toPosition)

                    // Update positions in database
                    updateItemPositions()
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not implementing swipe
            }
        })
        itemTouchHelper.attachToRecyclerView(uncheckedItemsList)
    }

    private fun setupListeners() {
        // Set up add item functionality
        addItemButton.setOnClickListener {
            addNewItem()
        }

        // Set up enter key to add items
        newItemText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addNewItem()
                return@setOnEditorActionListener true
            }
            false
        }

        // Set up expand/collapse for checked items
        checkedItemsHeader.setOnClickListener {
            toggleCheckedItemsVisibility()
        }
    }

    private fun addNewItem() {
        val text = newItemText.text.toString().trim()
        if (text.isNotEmpty()) {
            // Calculate the next position for the new item
            val position = 0 // Add to the top

            // Create a new checklist item
            val newItem = ChecklistItem(
                text = text,
                isChecked = false,
                position = position
            )

            // Save to database
            CoroutineScope(Dispatchers.IO).launch {
                val itemId = repository.insertChecklistItem(newItem)

                // Wait for the database operation to complete
                withContext(Dispatchers.Main) {
                    loadItems() // Now call loadItems on the main thread after the insert completes
                }
            }
        }

        // Clear input field
        newItemText.text.clear()

        // Hide keyboard
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(newItemText.windowToken, 0)
    }

    private fun handleItemChecked(item: ChecklistItem, isChecked: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            // Update the item in the database
            val updatedItem = item.copy(isChecked = isChecked)
            repository.updateChecklistItem(updatedItem)

            // Reload items from database
            loadItems()
        }
    }

    private fun toggleCheckedItemsVisibility() {
        isCheckedItemsExpanded = !isCheckedItemsExpanded

        if (isCheckedItemsExpanded) {
            checkedItemsList.visibility = View.VISIBLE
            // Rotate arrow upward
            expandArrow.animate().rotation(180f).setDuration(200).start()
        } else {
            checkedItemsList.visibility = View.GONE
            // Rotate arrow downward
            expandArrow.animate().rotation(0f).setDuration(200).start()
        }
    }

    @SuppressLint("SetTextI18s")
    private fun updateCheckedItemsCount() {
        CoroutineScope(Dispatchers.Main).launch {
            val count = withContext(Dispatchers.IO) {
                repository.getCheckedItemsCount()
            }
            checkedItemsCount.text = "$count Checked items"
        }
    }

    // Update positions of all unchecked items
    private fun updateItemPositions() {
        CoroutineScope(Dispatchers.IO).launch {
            // Update positions in database based on current list order
            uncheckedItems.forEachIndexed { index, item ->
                repository.updateChecklistItemPosition(item.id, index)
            }
        }
    }

    // Load items from database
    @SuppressLint("NotifyDataSetChanged")
    fun loadItems() {
        CoroutineScope(Dispatchers.IO).launch {
            val fetchedUncheckedItems = repository.getUncheckedItems()
            val fetchedCheckedItems = repository.getCheckedItems()

            withContext(Dispatchers.Main) {
                // Update unchecked items
                uncheckedItems.clear()
                uncheckedItems.addAll(fetchedUncheckedItems)
                uncheckedAdapter.notifyDataSetChanged()

                // Update checked items
                checkedItems.clear()
                checkedItems.addAll(fetchedCheckedItems)
                checkedAdapter.notifyDataSetChanged()

                // Update checked items count
                updateCheckedItemsCount()
            }
        }
    }

    /**
     * Inner class for the checklist adapter
     */
    private inner class ChecklistAdapter(
        private val items: MutableList<ChecklistItem>,
        private val onCheckedChange: (ChecklistItem, Boolean) -> Unit
    ) : RecyclerView.Adapter<ChecklistAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkbox: CheckBox = view.findViewById(R.id.item_checkbox)
            val deleteButton: ImageView = view.findViewById(R.id.delete_button)
            val dragHandle: ImageView? = view.findViewById(R.id.drag_handle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_checklist, parent, false)
            return ViewHolder(view)
        }
        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            // Set checkbox text and state
            holder.checkbox.text = item.text
            holder.checkbox.isChecked = item.isChecked

            val textView = holder.itemView.findViewById<TextView>(R.id.item_text)
            textView.text = item.text

            // Handle checkbox state changes
            holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != item.isChecked) {
                    onCheckedChange(item, isChecked)
                }
            }

            // Handle delete button
            holder.deleteButton.setOnClickListener {
                if (position < items.size) {
                    // Delete from database
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.deleteChecklistItem(item.id)

                        // Update UI
                        withContext(Dispatchers.Main) {
                            items.removeAt(position)
                            notifyItemRemoved(position)
                            updateCheckedItemsCount()
                        }
                    }
                }
            }

            // Setup drag handle touch listener if it exists
            // Only enable drag handle for unchecked items
            holder.dragHandle?.apply {
                visibility = if (items == uncheckedItems) View.VISIBLE else View.INVISIBLE

                setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        if (items == uncheckedItems) {
                            itemTouchHelper.startDrag(holder)
                        }
                        true
                    } else false
                }
            }
        }

        override fun getItemCount() = items.size
    }
}