package com.gorunjinian.dbst.viewmodels

import androidx.lifecycle.*
import com.gorunjinian.dbst.data.DBT
import com.gorunjinian.dbst.data.DST
import com.gorunjinian.dbst.data.EntryRepository
import kotlinx.coroutines.launch

class EntryViewModel(private val repository: EntryRepository) : ViewModel() {

    private val _incomeEntries = MutableLiveData<List<DBT>>()
    val incomeEntries: LiveData<List<DBT>> get() = _incomeEntries

    private val _expenseEntries = MutableLiveData<List<DST>>()
    val expenseEntries: LiveData<List<DST>> get() = _expenseEntries

    fun insertIncome(entry: DBT) = viewModelScope.launch {
        repository.insertIncome(entry)
        _incomeEntries.postValue(repository.getAllIncome())
    }

    fun insertExpense(entry: DST) = viewModelScope.launch {
        repository.insertExpense(entry)
        _expenseEntries.postValue(repository.getAllExpense())
    }

    fun loadIncome() = viewModelScope.launch {
        _incomeEntries.postValue(repository.getAllIncome())
    }

    fun loadExpense() = viewModelScope.launch {
        _expenseEntries.postValue(repository.getAllExpense())
    }

    fun deleteIncome(entry: DBT) = viewModelScope.launch {
        repository.deleteIncome(entry.id) // Pass only the ID
        _incomeEntries.postValue(repository.getAllIncome())
    }

    fun deleteExpense(entry: DST) = viewModelScope.launch {
        repository.deleteExpense(entry.id) // Pass only the ID
        _expenseEntries.postValue(repository.getAllExpense())
    }

    fun getTotalLBP(): LiveData<Double> {
        val total = MutableLiveData<Double>()
        viewModelScope.launch {
            val incomeEntries = repository.getAllIncome()
            total.postValue(incomeEntries.sumOf { it.totalLBP }) // Sum of all totalLBP values
        }
        return total
    }

    fun getTotalExchangedLBP(): LiveData<Double> {
        val total = MutableLiveData<Double>()
        viewModelScope.launch {
            val expenseEntries = repository.getAllExpense()
            total.postValue(expenseEntries.sumOf { it.exchangedLBP }) // Sum of exchangedLBP from all expenses
        }
        return total
    }
}
