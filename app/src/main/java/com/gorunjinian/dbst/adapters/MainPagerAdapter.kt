package com.gorunjinian.dbst.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.gorunjinian.dbst.fragments.EntryFragment
import com.gorunjinian.dbst.fragments.ValidityFragment
import com.gorunjinian.dbst.fragments.TetherFragment
import com.gorunjinian.dbst.fragments.InfoFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 4  // total fragments you have

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> EntryFragment()
            1 -> ValidityFragment()
            2 -> TetherFragment()
            3 -> InfoFragment()
            else -> EntryFragment()
        }
    }
}