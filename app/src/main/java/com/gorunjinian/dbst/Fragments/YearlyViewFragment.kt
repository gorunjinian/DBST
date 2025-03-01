package com.gorunjinian.dbst.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gorunjinian.dbst.R

class YearlyViewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("FragmentLifecycle", "ExportDataFragment View Created") // Debug log
        return inflater.inflate(R.layout.fragment_yearly_view, container, false)
    }
}
