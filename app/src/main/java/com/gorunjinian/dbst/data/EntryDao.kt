package com.gorunjinian.dbst.data

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface EntryDao {

    //DBT and DST queries

    // Insert Income Entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(entry: DBT): Long

    // Insert Expense Entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(entry: DST): Long

    // Get All Income Entries (Sorted by Date Descending)
    @Query("SELECT * FROM DBT ORDER BY date DESC")
    fun getAllIncome(): List<DBT>  // Removed `suspend`, since Room handles it in a background thread.

    // Get All Expense Entries (Sorted by Date Descending)
    @Query("SELECT * FROM DST ORDER BY date DESC")
    fun getAllExpense(): List<DST>  // Removed `suspend`

    // Dynamically get all table names in the database
    @RawQuery
    suspend fun getAllTableNames(query: SupportSQLiteQuery): List<String>

    // Delete Income Entry using Query (Fixed Parameter Type)
    @Query("DELETE FROM DBT WHERE id = :id")
    suspend fun deleteIncome(id: Int)  // Changed from `DBT` to `Int`

    // Delete Expense Entry using Query (Fixed Parameter Type)
    @Query("DELETE FROM DST WHERE id = :id")
    suspend fun deleteExpense(id: Int)  // Changed from `DST` to `Int`

    // Delete All Income Entries
    @Query("DELETE FROM DBT")
    suspend fun deleteAllIncome()

    // Delete All Expense Entries
    @Query("DELETE FROM DST")
    suspend fun deleteAllExpense()


    // VBSTIN Queries

    // Insert VBSTIN Entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVbstIn(entry: VBSTIN): Long

    // Get All VBSTIN Entries (Sorted by Date Descending)
    @Query("SELECT * FROM VBSTIN ORDER BY date DESC")
    fun getAllVbstIn(): List<VBSTIN>

    // Delete a VBSTIN Entry by ID
    @Query("DELETE FROM VBSTIN WHERE id = :id")
    suspend fun deleteVbstIn(id: Int)

    // Delete All VBSTIN Entries
    @Query("DELETE FROM VBSTIN")
    suspend fun deleteAllVbstIn()


    // --- VBSTOUT Queries ---

    // Insert VBSTOUT Entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVbstOut(entry: VBSTOUT): Long

    // Get All VBSTOUT Entries (Sorted by Date Descending)
    @Query("SELECT * FROM VBSTOUT ORDER BY date DESC")
    fun getAllVbstOut(): List<VBSTOUT>

    // Delete a VBSTOUT Entry by ID
    @Query("DELETE FROM VBSTOUT WHERE id = :id")
    suspend fun deleteVbstOut(id: Int)

    // Delete All VBSTOUT Entries
    @Query("DELETE FROM VBSTOUT")
    suspend fun deleteAllVbstOut()
}
