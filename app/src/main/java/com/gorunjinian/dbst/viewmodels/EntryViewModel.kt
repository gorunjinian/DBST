package com.gorunjinian.dbst.viewmodels

import androidx.lifecycle.*
import com.gorunjinian.dbst.data.DBT
import com.gorunjinian.dbst.data.DST
import com.gorunjinian.dbst.data.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EntryViewModel(private val repository: AppRepository) : ViewModel() {

    private val _incomeEntries = MutableLiveData<List<DBT>>()
    val incomeEntries: LiveData<List<DBT>> get() = _incomeEntries

    private val _expenseEntries = MutableLiveData<List<DST>>()
    val expenseEntries: LiveData<List<DST>> get() = _expenseEntries

    fun insertIncome(entry: DBT) = viewModelScope.launch(Dispatchers.IO) {
        val newId = repository.insertIncome(entry)
        val savedEntry = entry.copy(id = newId.toInt())
        _lastInsertedIncome.postValue(savedEntry)
        loadIncome()
    }

    fun insertExpense(entry: DST) = viewModelScope.launch(Dispatchers.IO) {
        val newId = repository.insertExpense(entry)
        val savedEntry = entry.copy(id = newId.toInt())
        _lastInsertedExpense.postValue(savedEntry)
        loadExpense()
    }

    fun loadIncome() = viewModelScope.launch(Dispatchers.IO) {
        val incomeList = repository.getAllIncome()
        _incomeEntries.postValue(incomeList)
    }

    fun loadExpense() = viewModelScope.launch(Dispatchers.IO) {
        val expenseList = repository.getAllExpense()
        _expenseEntries.postValue(expenseList)
    }

    fun deleteIncome(entry: DBT) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteIncome(entry.id)
        loadIncome()
    }

    fun deleteExpense(entry: DST) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteExpense(entry.id)
        loadExpense()
    }

    // Last inserted entries for undo functionality
    private val _lastInsertedIncome = MutableLiveData<DBT>()
    val lastInsertedIncome: LiveData<DBT> get() = _lastInsertedIncome

    private val _lastInsertedExpense = MutableLiveData<DST>()
    val lastInsertedExpense: LiveData<DST> get() = _lastInsertedExpense
}
