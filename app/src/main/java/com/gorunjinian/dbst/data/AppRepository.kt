package com.gorunjinian.dbst.data

class AppRepository(private val appDao: AppDao) {

    // Income queries
    suspend fun insertIncome(entry: DBT) = appDao.insertIncome(entry)
    fun getAllIncome(): List<DBT> = appDao.getAllIncome()
    suspend fun deleteIncome(id: Int) = appDao.deleteIncome(id)

    // Expense queries
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
}
