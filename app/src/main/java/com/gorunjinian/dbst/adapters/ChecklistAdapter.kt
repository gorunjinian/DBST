package com.gorunjinian.dbst.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.data.ChecklistItem

// Define the interface outside the class
interface ItemTouchHelperAdapter {
    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean
    fun onItemDismiss(position: Int)
}

class ChecklistAdapter(
    private val items: List<ChecklistItem>,
    private val onItemChecked: (ChecklistItem) -> Unit,
    private val onItemDeleted: (ChecklistItem) -> Unit,
    var onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null
) : RecyclerView.Adapter<ChecklistAdapter.ViewHolder>(), ItemTouchHelperAdapter {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dragHandle: ImageView = view.findViewById(R.id.drag_handle)
        val checkbox: CheckBox = view.findViewById(R.id.item_checkbox)
        val itemText: TextView = view.findViewById(R.id.item_text)
        val deleteButton: ImageButton = view.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checklist, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < 0 || position >= items.size) {
            Log.e("ChecklistAdapter", "Invalid position: $position, items size: ${items.size}")
            return
        }

        val item = items[position]

        // Log for debugging
        Log.d("ChecklistAdapter", "Binding item at position $position: ${item.text}")

        holder.itemText.text = item.text
        holder.checkbox.isChecked = item.isChecked

        // Handle checkbox changes
        holder.checkbox.setOnClickListener {
            onItemChecked(item)
        }

        // Handle delete button clicks
        holder.deleteButton.setOnClickListener {
            onItemDeleted(item)
        }

        // Setup drag handle if drag functionality is enabled
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                onStartDrag?.invoke(holder)
            }
            false
        }
    }

    override fun getItemCount(): Int {
        val count = items.size
        Log.d("ChecklistAdapter", "getItemCount: $count")
        return count
    }

    // ItemTouchHelper.Adapter implementation for drag-and-drop
    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        // The actual movement is handled by the viewModel
        return true
    }

    override fun onItemDismiss(position: Int) {
        // Not implementing swipe-to-dismiss
    }
}