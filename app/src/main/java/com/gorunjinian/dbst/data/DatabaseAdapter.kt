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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_database_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        when (val entry = dataList[position]) {
            is DBT -> {
                // DBT: Date, Person, Amount, Rate, Type, Total LBP
                holder.dateTextView.text = entry.date
                holder.personTextView.text = entry.person
                holder.amountTextView.text = formatter.format(entry.amount.toInt())
                holder.rateTextView.text = formatter.format(entry.rate?.toInt() ?: 0)
                holder.typeTextView.text = entry.type
                holder.exchTextView.text = formatter.format(entry.totalLBP.toInt())
                // Hide the extra column
                holder.exchangedLBPTextView.visibility = View.GONE
                holder.exchTextView.visibility = View.VISIBLE
            }
            is DST -> {
                // DST: Date, Person, Expd, Exch, Rate, Type, Exchanged LBP
                holder.dateTextView.text = entry.date
                holder.personTextView.text = entry.person
                // Column 3: Expd
                holder.amountTextView.text = formatter.format(entry.amountExpensed.toInt())
                // Column 4: Exch
                holder.rateTextView.text = formatter.format(entry.amountExchanged.toInt())
                // Column 5: Rate
                holder.typeTextView.text = formatter.format(entry.rate?.toInt() ?: 0)
                // Column 6: Type
                holder.exchTextView.text = entry.type
                // Column 7: Exchanged LBP
                holder.exchangedLBPTextView.text = formatter.format(entry.exchangedLBP.toInt())
                // Ensure all columns are visible
                holder.exchangedLBPTextView.visibility = View.VISIBLE
                holder.exchTextView.visibility = View.VISIBLE
            }
            is VBSTIN -> {
                // VBSTIN: Date, Person, Type, Validity, Amount, Total, Rate
                holder.dateTextView.text = entry.date
                holder.personTextView.text = entry.person
                // Column 3: Type
                holder.amountTextView.text = entry.type
                // Column 4: Validity
                holder.rateTextView.text = entry.validity
                // Column 5: Amount
                holder.typeTextView.text = formatter.format(entry.amount.toInt())
                // Column 6: Total
                holder.exchTextView.text = formatter.format(entry.total.toInt())
                // Column 7: Rate
                holder.exchangedLBPTextView.text = formatter.format(entry.rate.toInt())
                // Ensure all columns are visible
                holder.amountTextView.visibility = View.VISIBLE
                holder.rateTextView.visibility = View.VISIBLE
                holder.typeTextView.visibility = View.VISIBLE
                holder.exchTextView.visibility = View.VISIBLE
                holder.exchangedLBPTextView.visibility = View.VISIBLE
            }
            is VBSTOUT -> {
                // VBSTOUT: Date, Person, Amount, SellRate, Type, Profit
                holder.dateTextView.text = entry.date
                holder.personTextView.text = entry.person
                // Column 3: Amount
                holder.amountTextView.text = formatter.format(entry.amount.toInt())
                // Column 4: SellRate
                holder.rateTextView.text = formatter.format(entry.sellrate.toInt())
                // Column 5: Type
                holder.typeTextView.text = entry.type
                // Column 6: Profit
                holder.exchTextView.text = formatter.format(entry.profit.toInt())
                // Hide the extra column (Column 7)
                holder.exchangedLBPTextView.visibility = View.GONE
                holder.amountTextView.visibility = View.VISIBLE
                holder.rateTextView.visibility = View.VISIBLE
                holder.typeTextView.visibility = View.VISIBLE
                holder.exchTextView.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int = dataList.size
}