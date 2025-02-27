package com.gorunjinian.dbst.data

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface EntryDao {

    // ✅ Insert Income Entry

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(entry: DBT)

    // ✅ Insert Expense Entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(entry: DST)

    // ✅ Get All Income Entries (Sorted by Date Descending)
    @Query("SELECT * FROM DBT ORDER BY date DESC")
    fun getAllIncome(): List<DBT>  // Removed `suspend`, since Room handles it in a background thread.

    // ✅ Get All Expense Entries (Sorted by Date Descending)
    @Query("SELECT * FROM DST ORDER BY date DESC")
    fun getAllExpense(): List<DST>  // Removed `suspend`

    // ✅ Dynamically get all table names in the database
    @RawQuery
    suspend fun getAllTableNames(query: SupportSQLiteQuery): List<String>

    // ✅ Delete Income Entry using Query (Fixed Parameter Type)
    @Query("DELETE FROM DBT WHERE id = :id")
    suspend fun deleteIncome(id: Int)  // Changed from `DBT` to `Int`

    // ✅ Delete Expense Entry using Query (Fixed Parameter Type)
    @Query("DELETE FROM DST WHERE id = :id")
    suspend fun deleteExpense(id: Int)  // Changed from `DST` to `Int`

    // ✅ Delete All Income Entries
    @Query("DELETE FROM DBT")
    suspend fun deleteAllIncome()

    // ✅ Delete All Expense Entries
    @Query("DELETE FROM DST")
    suspend fun deleteAllExpense()
}
