package com.gorunjinian.dbst.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DBT")
data class DBT(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val person: String,
    val amount: Double,
    val rate: Double?,
    val type: String,
    val totalLBP: Double = amount * (rate ?: 1.0) // Default rate to 1.0 if null
)

@Entity(tableName = "DST")
data class DST(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val person: String,
    val amountExpensed: Double,
    val amountExchanged: Double,
    val rate: Double?,
    val type: String,
    val exchangedLBP: Double = amountExchanged * (rate ?: 1.0) // Default rate to 1.0 if null
)

@Entity(tableName = "VBSTIN")
data class VBSTIN(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val person: String,
    val type: String,
    val validity: String,
    val amount: Double,
    val total: Double,
    val rate: Double = total / amount
)

@Entity(tableName = "VBSTOUT")
data class VBSTOUT(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val person: String,
    val amount: Double,
    val sellrate: Double,
    val type: String,
    val profit: Double
)

@Entity(tableName = "USDT")
data class USDT(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val person: String,
    val amountUsdt: Double,
    val amountCash : Double,
    val type: String,
)

@Entity(tableName = "cash_counter")
data class CashCounter(
    @PrimaryKey val id: Int = 1, // Single instance approach
    val ones: Int = 0,
    val twos: Int = 0,
    val fives: Int = 0,
    val tens: Int = 0,
    val twenties: Int = 0,
    val fifties: Int = 0,
    val hundreds: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_givens")
data class UserGivens(
    @PrimaryKey val id: Int = 1,
    val usdLbpBmr: Int = 0,
    val usdLbpVld: Int = 0,
    val usdLbpSr: Int = 0,
    val vldPrice: Int = 0,
    val vldProfit: Double = 0.00,
    val btcPrice: Int = 0,
    val btcExch1Balance: Double = 0.00000000,
    val btcExch2Balance: Double = 0.00000000,
    val btcWltBalance: Double = 0.00000000,
    val altExch1Balance: Int = 0,
    val altExch2Balance: Int = 0,
    val altWltBalance: Int = 0,
    val whishBalance: Int = 0,
    val stocksBalance: Int = 0,
    val usdtBalance: Int = 0,
    val cashLbpBalance: Int = 0,
    val bankBalance: Int = 0,
    val usdtDebt: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "checklist_items")
    data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    var isChecked: Boolean = false,
    val position: Int, // For maintaining order
    val dateCreated: Long = System.currentTimeMillis()
)