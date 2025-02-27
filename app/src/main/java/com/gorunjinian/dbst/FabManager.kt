package com.gorunjinian.dbst

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton

@SuppressLint("StaticFieldLeak")
object FabManager {

    private lateinit var inputFields: List<EditText>
    private lateinit var totalAmountEditText: TextView

    fun setupFab(fab: FloatingActionButton, viewPager: ViewPager2, activity: FragmentActivity) {
        fab.setOnClickListener {
            showPopup(activity) // Always open the popup
        }
    }

    @SuppressLint("SetTextI18n")
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

        // Initialize input fields
        inputFields = listOf(
            dialog.findViewById(R.id.input_1),
            dialog.findViewById(R.id.input_2),
            dialog.findViewById(R.id.input_5),
            dialog.findViewById(R.id.input_10),
            dialog.findViewById(R.id.input_20),
            dialog.findViewById(R.id.input_50),
            dialog.findViewById(R.id.input_100)
        )

        totalAmountEditText = dialog.findViewById(R.id.total_amount)
        totalAmountEditText.text = "$0"

        // ✅ Add text change listeners to update total dynamically
        val denominationValues = listOf(1, 2, 5, 10, 20, 50, 100)
        inputFields.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    updateTotal(denominationValues)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        // Close button functionality
        val closeButton: Button = dialog.findViewById(R.id.close_popup)
        closeButton.setOnClickListener { dialog.dismiss() }

        // Adjust popup window size
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun updateTotal(values: List<Int>) {
        var total = 0
        inputFields.forEachIndexed { index, editText ->
            val input = editText.text.toString()
            if (input.isNotEmpty()) {
                total += input.toInt() * values[index]
            }
        }
        totalAmountEditText.text = "$" + String.format("%,d", total) // Format with commas
    }
}