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