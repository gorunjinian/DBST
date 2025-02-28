package com.gorunjinian.dbst.data

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gorunjinian.dbst.R
import java.text.NumberFormat
import java.util.Locale

class DatabaseAdapter : RecyclerView.Adapter<DatabaseAdapter.ViewHolder>() {

    private var dataList: List<Any> = emptyList()
    private var currentTable: String = "DBT" // Track table type for correct formatting

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(table: String, data: List<Any>) {
        currentTable = table
        dataList = data
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.col_date)
        val personTextView: TextView = itemView.findViewById(R.id.col_person)
        val amountTextView: TextView = itemView.findViewById(R.id.col_amount)
        val rateTextView: TextView = itemView.findViewById(R.id.col_rate)
        val typeTextView: TextView = itemView.findViewById(R.id.col_type)
        val exchTextView: TextView = itemView.findViewById(R.id.col_exch)
        val exchangedLBPTextView: TextView = itemView.findViewById(R.id.col_exchanged_lbp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_database_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        when (val entry = dataList[position]) {
            is DBT -> {
                holder.dateTextView.text = entry.date
                holder.personTextView.text = entry.person
                holder.amountTextView.text = formatter.format(entry.amount.toInt()) // Format with commas, no decimals
                holder.rateTextView.text = formatter.format(entry.rate?.toInt() ?: 0)
                holder.typeTextView.text = entry.type
                holder.exchangedLBPTextView.text = formatter.format(entry.totalLBP.toInt())

                // Hide Exch column (amountExchanged) for DBT
                holder.exchTextView.visibility = View.GONE
            }
            is DST -> {
                holder.dateTextView.text = entry.date
                holder.personTextView.text = entry.person
                holder.amountTextView.text = formatter.format(entry.amountExpensed.toInt()) //
                // Format with commas, no decimals
                holder.rateTextView.text = formatter.format(entry.rate?.toInt() ?: 0)
                holder.typeTextView.text = entry.type
                holder.exchTextView.text = formatter.format(entry.amountExchanged.toInt()) // Show amount exchanged
                holder.exchangedLBPTextView.text = formatter.format(entry.exchangedLBP.toInt())

                // Ensure Exch column is visible for DST
                holder.exchTextView.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int = dataList.size
}