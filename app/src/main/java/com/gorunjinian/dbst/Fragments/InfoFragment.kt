package com.gorunjinian.dbst.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.gorunjinian.dbst.R

class InfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_info, container, false)
    }
}
