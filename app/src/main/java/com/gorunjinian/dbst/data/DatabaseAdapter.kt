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

    var onRecordLongClickListener: ((Any) -> Unit)? = null

    // Add this to DatabaseAdapter class
    var onRecordClickListener: ((Any) -> Unit)? = null


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

    @SuppressLint("SetTextI18s")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        // Clear any previous dynamic views
        holder.rowContainer.removeAllViews()

        // In onBindViewHolder of DatabaseAdapter
        holder.itemView.setOnClickListener {
            onRecordClickListener?.invoke(dataList[position])
        }


        val item = dataList[position]

        // Get properties using reflection
        @Suppress("UNCHECKED_CAST")
        val memberProps = item::class.memberProperties.toList() as List<KProperty1<Any, *>>

        // Convert to a map of property names to values
        val propMap = mutableMapOf<String, Any?>()
        memberProps.forEach { prop ->
            propMap[prop.name] = prop.get(item)
        }

        // Define column order (same as in fragment)
        val priorityOrder = listOf(
            "id", "date", "person",
            "amount", "amountExpensed", "amountExchanged", "amountUsdt", "amountCash",
            "rate", "sellrate",
            "type", "validity",
            "totalLBP", "exchangedLBP", "profit", "total"
        )

        // Create text views for each property in the correct order
        priorityOrder.forEach { propName ->
            if (propMap.containsKey(propName)) {
                val value = propMap[propName]
                val textValue = when (value) {
                    is Number -> formatter.format(value)
                    else -> value?.toString() ?: ""
                }

                val textView = TextView(context).apply {
                    text = textValue
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        marginStart = 2
                        marginEnd = 2
                    }
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }
                holder.rowContainer.addView(textView)
            }
        }

        // Set long press listener to notify the host fragment about this record
        holder.itemView.setOnLongClickListener {
            onRecordLongClickListener?.invoke(dataList[position])
            true
        }
    }


    override fun getItemCount(): Int = dataList.size


    fun getDisplayNameForProperty(propertyName: String): String {
        return when (entityType) {
            "DBT" -> when (propertyName) {
                "date" -> "Date"
                "person" -> "Person"
                "amount" -> "Amount"
                "rate" -> "Rate"
                "type" -> "Type"
                "totalLBP" -> "Tot"
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