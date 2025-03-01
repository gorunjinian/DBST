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
    private var currentTable: String = "DBT" // Used to determine which mapping to use

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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        when (val entry = dataList[position]) {
            is DBT -> {
                // Expected Headers: Date, Person, Amount, Rate, Type, Total LBP
                holder.dateTextView.text = entry.date
                holder.personTextView.text = entry.person
                holder.amountTextView.text = formatter.format(entry.amount.toInt())
                holder.rateTextView.text = formatter.format(entry.rate?.toInt() ?: 0)
                // Swap the next two because our layout’s order is reversed:
                holder.exchTextView.text = entry.type
                holder.typeTextView.text = formatter.format(entry.totalLBP.toInt())
                holder.exchangedLBPTextView.visibility = View.GONE
            }
            is DST -> {
                // Expected Headers: Date, Person, Expd, Exch, Rate, Type, Exchanged LBP
                holder.dateTextView.text = entry.date
                holder.personTextView.text = entry.person
                holder.amountTextView.text = formatter.format(entry.amountExpensed.toInt())
                holder.rateTextView.text = formatter.format(entry.amountExchanged.toInt())
                // Swap the next two:
                holder.exchTextView.text = formatter.format(entry.rate?.toInt() ?: 0)
                holder.typeTextView.text = entry.type
                holder.exchangedLBPTextView.text = formatter.format(entry.exchangedLBP.toInt())
                holder.exchangedLBPTextView.visibility = View.VISIBLE
            }
            is VBSTIN -> {
                // Expected Headers: Date, Person, Type, Validity, Amount, Total, Rate
                holder.dateTextView.text = entry.date
                holder.personTextView.text = entry.person
                holder.amountTextView.text = entry.type        // Column3: Type
                holder.rateTextView.text = entry.validity        // Column4: Validity
                holder.typeTextView.text = formatter.format(entry.amount.toInt()) // Column5: Amount
                holder.exchTextView.text = formatter.format(entry.total.toInt())    // Column6: Total
                holder.exchangedLBPTextView.text = formatter.format(entry.rate.toInt()) // Column7: Rate
                holder.exchangedLBPTextView.visibility = View.VISIBLE
            }
            is VBSTOUT -> {
                // Expected Headers: Date, Person, Amount, SellRate, Type, Profit
                holder.dateTextView.text = entry.date
                holder.personTextView.text = entry.person
                holder.amountTextView.text = formatter.format(entry.amount.toInt())
                // Swap the next two to correct the error:
                holder.rateTextView.text = formatter.format(entry.sellrate.toInt()) // Column4: SellRate
                holder.typeTextView.text = entry.type                                  // Column5: Type
                holder.exchTextView.text = formatter.format(entry.profit.toInt())        // Column6: Profit
                holder.exchangedLBPTextView.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = dataList.size
}