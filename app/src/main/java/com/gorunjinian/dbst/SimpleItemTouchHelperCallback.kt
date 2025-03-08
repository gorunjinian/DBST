package com.gorunjinian.dbst

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView


class SimpleItemTouchHelperCallback(
    private val callback: ItemTouchHelper.Callback
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
) {
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return callback.onMove(recyclerView, viewHolder, target)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        callback.onSwiped(viewHolder, direction)
    }
}