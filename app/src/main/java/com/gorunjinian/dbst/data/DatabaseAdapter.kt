package com.gorunjinian.dbst.data

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gorunjinian.dbst.R
import java.text.NumberFormat
import java.util.Locale
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class DatabaseAdapter : RecyclerView.Adapter<DatabaseAdapter.ViewHolder>() {

    private var dataList: List<Any> = emptyList()
    // Track the entity type to use for column name mapping
    private var entityType: String = ""

    // Long press callback to notify the host fragment
    var onRecordLongClickListener: ((Any) -> Unit)? = null

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(data: List<Any>) {
        dataList = data

        // Determine entity type from the first item if available
        if (data.isNotEmpty()) {
            entityType = when (data[0].javaClass.simpleName) {
                "DBT" -> "DBT"
                "DST" -> "DST"
                "VBSTIN" -> "VBSTIN"
                "VBSTOUT" -> "VBSTOUT"
                "USDT" -> "USDT"
                else -> ""
            }
        }

        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rowContainer: LinearLayout = itemView.findViewById(R.id.row_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the dynamic row layout
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_database_record, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        // Clear any previous dynamic views
        holder.rowContainer.removeAllViews()

        @Suppress("UNCHECKED_CAST")
        val props = dataList[position]::class.memberProperties.toList() as List<KProperty1<Any, *>>
        props.forEach { prop: KProperty1<Any, *> ->
            val textValue = when (val value = prop.get(dataList[position])) {
                is Number -> formatter.format(value)
                else -> value?.toString() ?: ""
            }
            val textView = TextView(context).apply {
                text = textValue
                gravity = Gravity.CENTER
                // Reduce text size to fit single line
                textSize = 12f  // Smaller text size
                maxLines = 1  // Ensure single line
                ellipsize = TextUtils.TruncateAt.END  // Add ellipsis if too long
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = 4
                    marginEnd = 4
                }
            }
            holder.rowContainer.addView(textView)
        }

        // Set long press listener to notify the host fragment about this record
        holder.itemView.setOnLongClickListener {
            onRecordLongClickListener?.invoke(dataList[position])
            true
        }
    }

    override fun getItemCount(): Int = dataList.size

    // Helper function to get the display name for a property
    fun getDisplayNameForProperty(propertyName: String): String {
        return when (entityType) {
            "DBT" -> when (propertyName) {
                "date" -> "Date"
                "person" -> "Person"
                "amount" -> "Amount"
                "rate" -> "Rate"
                "type" -> "Type"
                "totalLBP" -> "Tot LBP"
                else -> propertyName.replaceFirstChar { it.uppercase() }
            }
            "DST" -> when (propertyName) {
                "date" -> "Date"
                "person" -> "Person"
                "amountExpensed" -> "Expd"
                "amountExchanged" -> "Exch"
                "rate" -> "Rate"
                "type" -> "Type"
                "exchangedLBP" -> "Tot LBP"
                else -> propertyName.replaceFirstChar { it.uppercase() }
            }
            "VBSTIN" -> when (propertyName) {
                "date" -> "Date"
                "person" -> "Person"
                "type" -> "Type"
                "validity" -> "Validity"
                "amount" -> "Amount"
                "total" -> "Total"
                "rate" -> "Rate"
                else -> propertyName.replaceFirstChar { it.uppercase() }
            }
            "VBSTOUT" -> when (propertyName) {
                "date" -> "Date"
                "person" -> "Person"
                "amount" -> "Amount $"
                "sellrate" -> "Sell R"
                "type" -> "Type"
                "profit" -> "Profit"
                else -> propertyName.replaceFirstChar { it.uppercase() }
            }
            "USDT" -> when (propertyName) {
                "date" -> "Date"
                "person" -> "Person"
                "amountUsdt" -> "USDT"
                "amountCash" -> "Cash"
                "type" -> "Type"
                else -> propertyName.replaceFirstChar { it.uppercase() }
            }
            else -> propertyName.replaceFirstChar { it.uppercase() }
        }
    }
}