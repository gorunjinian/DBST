package com.gorunjinian.dbst

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for the popup ViewPager2
 */
class PopupPagerAdapter(private val layouts: List<Int>) :
    RecyclerView.Adapter<PopupPagerAdapter.PagerViewHolder>() {

    class PagerViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        val layout = layouts[viewType]
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return PagerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        // No binding needed for static layouts
    }

    override fun getItemCount(): Int = layouts.size

    override fun getItemViewType(position: Int): Int = position
}