package com.gorunjinian.dbst.data

class EntryRepository(private val entryDao: EntryDao) {

    // Income queries
    suspend fun insertIncome(entry: DBT) = entryDao.insertIncome(entry)
    fun getAllIncome(): List<DBT> = entryDao.getAllIncome()
    suspend fun deleteIncome(id: Int) = entryDao.deleteIncome(id)

    // Expense queries
    suspend fun insertExpense(entry: DST) = entryDao.insertExpense(entry)
    fun getAllExpense(): List<DST> = entryDao.getAllExpense()
    suspend fun deleteExpense(id: Int) = entryDao.deleteExpense(id)

    // VBSTIN queries
    suspend fun insertVbstIn(entry: VBSTIN) = entryDao.insertVbstIn(entry)
    fun getAllVbstIn(): List<VBSTIN> = entryDao.getAllVbstIn()
    suspend fun deleteVbstIn(id: Int) = entryDao.deleteVbstIn(id)
    suspend fun deleteAllVbstIn() = entryDao.deleteAllVbstIn()

    // VBSTOUT queries
    suspend fun insertVbstOut(entry: VBSTOUT) = entryDao.insertVbstOut(entry)
    fun getAllVbstOut(): List<VBSTOUT> = entryDao.getAllVbstOut()
    suspend fun deleteVbstOut(id: Int) = entryDao.deleteVbstOut(id)
    suspend fun deleteAllVbstOut() = entryDao.deleteAllVbstOut()
}
