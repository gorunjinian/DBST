package com.gorunjinian.dbst.viewmodels

import androidx.lifecycle.*
import com.gorunjinian.dbst.data.USDT
import com.gorunjinian.dbst.data.AppRepository
import com.gorunjinian.dbst.data.DST
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TetherViewModel(private val repository: AppRepository) : ViewModel() {

    private val _usdtEntries = MutableLiveData<List<USDT>>()
    val usdtEntries: LiveData<List<USDT>> get() = _usdtEntries

    // Last inserted entry for undo functionality
    private val _lastInsertedUsdt = MutableLiveData<USDT>()
    val lastInsertedUsdt: LiveData<USDT> get() = _lastInsertedUsdt

    fun insertUsdt(entry: USDT) = viewModelScope.launch(Dispatchers.IO) {
        val newId = repository.insertUsdt(entry)
        val savedEntry = entry.copy(id = newId.toInt())
        _lastInsertedUsdt.postValue(savedEntry)
        loadUsdt()
    }

    fun insertUsdtWithWhishOption(entry: USDT, isPaidByWhish: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        val newId = repository.insertUsdt(entry)
        val savedEntry = entry.copy(id = newId.toInt())

        // If paid by WHISH, create expense entry
        if (isPaidByWhish) {
            val expenseEntry = DST(
                date = entry.date,
                person = entry.person,
                amountExpensed = entry.amountCash,
                amountExchanged = 0.0,
                rate = 0.0, // Default rate
                type = "WHISH TOPUP"
            )
            repository.insertExpense(expenseEntry)
        }

        _lastInsertedUsdt.postValue(savedEntry)
        loadUsdt()
    }

    fun loadUsdt() = viewModelScope.launch(Dispatchers.IO) {
        val entries = repository.getAllUsdt()
        _usdtEntries.postValue(entries)
    }

    fun deleteUsdt(entry: USDT) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteUsdt(entry.id)
        loadUsdt()
    }

    fun deleteAllUsdt() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAllUsdt()
        repository.resetUsdtSequence()
        loadUsdt()
    }
}