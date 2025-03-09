package com.gorunjinian.dbst.data

import androidx.sqlite.db.SupportSQLiteQuery

class AppRepository(private val appDao: AppDao) {

    // all table names getter
    suspend fun getAllTableNames(query: SupportSQLiteQuery): List<String> = appDao.getAllTableNames(query)

    // DBT queries
    suspend fun insertIncome(entry: DBT) = appDao.insertIncome(entry)
    fun getAllIncome(): List<DBT> = appDao.getAllIncome()
    suspend fun deleteIncome(id: Int) = appDao.deleteIncome(id)

    // DST queries
    suspend fun insertExpense(entry: DST) = appDao.insertExpense(entry)
    fun getAllExpense(): List<DST> = appDao.getAllExpense()
    suspend fun deleteExpense(id: Int) = appDao.deleteExpense(id)

    // VBSTIN queries
    suspend fun insertVbstIn(entry: VBSTIN) = appDao.insertVbstIn(entry)
    fun getAllVbstIn(): List<VBSTIN> = appDao.getAllVbstIn()
    suspend fun deleteVbstIn(id: Int) = appDao.deleteVbstIn(id)
    suspend fun deleteAllVbstIn() = appDao.deleteAllVbstIn()

    // VBSTOUT queries
    suspend fun insertVbstOut(entry: VBSTOUT) = appDao.insertVbstOut(entry)
    fun getAllVbstOut(): List<VBSTOUT> = appDao.getAllVbstOut()
    suspend fun deleteVbstOut(id: Int) = appDao.deleteVbstOut(id)
    suspend fun deleteAllVbstOut() = appDao.deleteAllVbstOut()

    // Add methods for resetting sequence counters
    suspend fun resetDbtSequence() = appDao.resetDbtSequence()
    suspend fun resetDstSequence() = appDao.resetDstSequence()
    suspend fun resetVbstInSequence() = appDao.resetVbstInSequence()
    suspend fun resetVbstOutSequence() = appDao.resetVbstOutSequence()
    suspend fun resetAllSequences() = appDao.resetAllSequences()


    // USDT queries
    suspend fun insertUsdt(entry: USDT) = appDao.insertUsdt(entry)
    fun getAllUsdt(): List<USDT> = appDao.getAllUsdt()
    suspend fun deleteUsdt(id: Int) = appDao.deleteUsdt(id)
    suspend fun deleteAllUsdt() = appDao.deleteAllUsdt()
    suspend fun resetUsdtSequence() = appDao.resetUsdtSequence()

    //Cash counter
    suspend fun getCashCounter(): CashCounter? = appDao.getCashCounter()
    suspend fun saveCashCounter(cashCounter: CashCounter) = appDao.insertCashCounter(cashCounter)

    // Checklist items methods
    fun getUncheckedItems(): List<ChecklistItem> = appDao.getUncheckedItems()
    fun getCheckedItems(): List<ChecklistItem> = appDao.getCheckedItems()
    suspend fun insertChecklistItem(item: ChecklistItem) = appDao.insertChecklistItem(item)
    suspend fun updateChecklistItem(item: ChecklistItem) = appDao.updateChecklistItem(item)
    suspend fun deleteChecklistItem(id: Int) = appDao.deleteChecklistItem(id)
    suspend fun updateChecklistItemPosition(id: Int, newPosition: Int) = appDao.updateChecklistItemPosition(id, newPosition)
    suspend fun getCheckedItemsCount(): Int = appDao.getCheckedItemsCount()
}
