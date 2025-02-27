package com.gorunjinian.dbst.data

class EntryRepository(private val entryDao: EntryDao) {

    suspend fun insertIncome(entry: DBT) = entryDao.insertIncome(entry)

    suspend fun insertExpense(entry: DST) = entryDao.insertExpense(entry)

    fun getAllIncome(): List<DBT> = entryDao.getAllIncome()

    fun getAllExpense(): List<DST> = entryDao.getAllExpense()

    suspend fun deleteIncome(id: Int) = entryDao.deleteIncome(id)  // Fixed parameter

    suspend fun deleteExpense(id: Int) = entryDao.deleteExpense(id)  // Fixed parameter
}
