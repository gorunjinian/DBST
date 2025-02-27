package com.gorunjinian.dbst.data

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gorunjinian.dbst.R

class DatabaseAdapter : RecyclerView.Adapter<DatabaseAdapter.ViewHolder>() {

    private var dataList: List<Any> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(table: String, data: List<Any>) {
        dataList = data
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.col_date)
        private val personTextView: TextView = itemView.findViewById(R.id.col_person)
        private val amountTextView: TextView = itemView.findViewById(R.id.col_amount)
        private val rateTextView: TextView = itemView.findViewById(R.id.col_rate)
        private val typeTextView: TextView = itemView.findViewById(R.id.col_type)
        private val exchTextView: TextView = itemView.findViewById(R.id.col_exch) // Exch Column
        private val exchangedLBPTextView: TextView = itemView.findViewById(R.id.col_exchanged_lbp)

        @SuppressLint("SetTextI18n")
        fun bind(entry: Any) {
            when (entry) {
                is DBT -> {
                    dateTextView.text = entry.date
                    personTextView.text = entry.person
                    amountTextView.text = entry.amount.toString()
                    rateTextView.text = entry.rate?.toString() ?: "-"
                    typeTextView.text = entry.type
                    exchangedLBPTextView.text = entry.totalLBP.toString()

                    // ✅ Hide Exch column for DBT since it's not needed
                    exchTextView.visibility = View.GONE
                }
                is DST -> {
                    dateTextView.text = entry.date
                    personTextView.text = entry.person
                    amountTextView.text = entry.amountExpensed.toString()
                    rateTextView.text = entry.rate?.toString() ?: "-"
                    typeTextView.text = entry.type
                    exchTextView.text = entry.amountExchanged.toString() // ✅ Show correct exchanged amount
                    exchangedLBPTextView.text = entry.exchangedLBP.toString()

                    // Ensure Exch column is visible for DST records
                    exchTextView.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_database_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }

    override fun getItemCount(): Int = dataList.size
}