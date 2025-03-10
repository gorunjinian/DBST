package com.gorunjinian.dbst.data

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface AppDao {

    //DBT and DST queries

    // Insert Income Entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(entry: DBT): Long

    // Insert Expense Entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(entry: DST): Long

    // Get All Income Entries (Sorted by id Descending)
    @Query("SELECT * FROM DBT ORDER BY id DESC")
    fun getAllIncome(): List<DBT>

    // Get All Expense Entries (Sorted by Date Descending)
    @Query("SELECT * FROM DST ORDER BY id DESC")
    fun getAllExpense(): List<DST>

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

    // Get the maximum ID in the DBT table
    @Query("SELECT MAX(id) FROM DBT")
    suspend fun getMaxDbtId(): Int

    // Reset sequence to continue from a specific value
    @Query("UPDATE sqlite_sequence SET seq = :maxId WHERE name = 'DBT'")
    suspend fun resetDbtSequenceTo(maxId: Int)


    // VBSTIN Queries

    // Insert VBSTIN Entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVbstIn(entry: VBSTIN): Long

    // Get All VBSTIN Entries (Sorted by Date Descending)
    @Query("SELECT * FROM VBSTIN ORDER BY id DESC")
    fun getAllVbstIn(): List<VBSTIN>

    // Delete a VBSTIN Entry by ID
    @Query("DELETE FROM VBSTIN WHERE id = :id")
    suspend fun deleteVbstIn(id: Int)

    // Delete All VBSTIN Entries
    @Query("DELETE FROM VBSTIN")
    suspend fun deleteAllVbstIn()


    // VBSTOUT Queries

    // Insert VBSTOUT Entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVbstOut(entry: VBSTOUT): Long

    // Get All VBSTOUT Entries (Sorted by Date Descending)
    @Query("SELECT * FROM VBSTOUT ORDER BY id DESC")
    fun getAllVbstOut(): List<VBSTOUT>

    // Delete a VBSTOUT Entry by ID
    @Query("DELETE FROM VBSTOUT WHERE id = :id")
    suspend fun deleteVbstOut(id: Int)

    // Delete All VBSTOUT Entries
    @Query("DELETE FROM VBSTOUT")
    suspend fun deleteAllVbstOut()

    // Reset sequence for specific tables
    @Query("DELETE FROM sqlite_sequence WHERE name = 'DBT'")
    suspend fun resetDbtSequence()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'DST'")
    suspend fun resetDstSequence()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'VBSTIN'")
    suspend fun resetVbstInSequence()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'VBSTOUT'")
    suspend fun resetVbstOutSequence()

    // Reset all sequences method
    @Query("DELETE FROM sqlite_sequence")
    suspend fun resetAllSequences()

    // USDT Queries

    // Insert USDT Entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsdt(entry: USDT): Long

    // Get All USDT Entries (Sorted by Date Descending)
    @Query("SELECT * FROM USDT ORDER BY id DESC")
    fun getAllUsdt(): List<USDT>

    // Delete a USDT Entry by ID
    @Query("DELETE FROM USDT WHERE id = :id")
    suspend fun deleteUsdt(id: Int)

    // Delete All USDT Entries
    @Query("DELETE FROM USDT")
    suspend fun deleteAllUsdt()

    // Reset USDT sequence
    @Query("DELETE FROM sqlite_sequence WHERE name = 'USDT'")
    suspend fun resetUsdtSequence()


    @Query("SELECT MAX(id) FROM DST")
    suspend fun getMaxDstId(): Int

    @Query("UPDATE sqlite_sequence SET seq = :maxId WHERE name = 'DST'")
    suspend fun resetDstSequenceTo(maxId: Int)


    @Query("SELECT MAX(id) FROM VBSTIN")
    suspend fun getMaxVbstInId(): Int

    @Query("UPDATE sqlite_sequence SET seq = :maxId WHERE name = 'VBSTIN'")
    suspend fun resetVbstInSequenceTo(maxId: Int)


    @Query("SELECT MAX(id) FROM VBSTOUT")
    suspend fun getMaxVbstOutId(): Int

    @Query("UPDATE sqlite_sequence SET seq = :maxId WHERE name = 'VBSTOUT'")
    suspend fun resetVbstOutSequenceTo(maxId: Int)


    @Query("SELECT MAX(id) FROM USDT")
    suspend fun getMaxUsdtId(): Int

    @Query("UPDATE sqlite_sequence SET seq = :maxId WHERE name = 'USDT'")
    suspend fun resetUsdtSequenceTo(maxId: Int)

    // Cash Counter queries

    @Query("SELECT * FROM cash_counter WHERE id = 1")
    suspend fun getCashCounter(): CashCounter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashCounter(cashCounter: CashCounter)

    //user Given queries

    @Query("SELECT * FROM user_givens WHERE id = 1")
    suspend fun getUserGivens(): UserGivens?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserGivens(userGivens: UserGivens)


    // Add these methods to your existing AppDao interface

    // Get all checklist items sorted by position
    @Query("SELECT * FROM checklist_items WHERE isChecked = 0 ORDER BY position ASC")
    fun getUncheckedItems(): List<ChecklistItem>

    @Query("SELECT * FROM checklist_items WHERE isChecked = 1 ORDER BY dateCreated DESC")
    fun getCheckedItems(): List<ChecklistItem>

    // Insert a new checklist item
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistItem): Long

    // Update a checklist item
    @Update
    suspend fun updateChecklistItem(item: ChecklistItem)

    // Delete a checklist item
    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteChecklistItem(id: Int)

    // Update positions of multiple items (for reordering)
    @Query("UPDATE checklist_items SET position = :newPosition WHERE id = :id")
    suspend fun updateChecklistItemPosition(id: Int, newPosition: Int)

    // Count checked items
    @Query("SELECT COUNT(*) FROM checklist_items WHERE isChecked = 1")
    suspend fun getCheckedItemsCount(): Int

}
