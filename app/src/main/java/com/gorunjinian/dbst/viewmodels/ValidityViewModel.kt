package com.gorunjinian.dbst.viewmodels

import androidx.lifecycle.*
import com.gorunjinian.dbst.data.VBSTIN
import com.gorunjinian.dbst.data.VBSTOUT
import com.gorunjinian.dbst.data.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ValidityViewModel(private val repository: AppRepository) : ViewModel() {

    private val _vbstInEntries = MutableLiveData<List<VBSTIN>>()
    val vbstInEntries: LiveData<List<VBSTIN>> get() = _vbstInEntries

    private val _vbstOutEntries = MutableLiveData<List<VBSTOUT>>()
    val vbstOutEntries: LiveData<List<VBSTOUT>> get() = _vbstOutEntries

    // VBSTIN operations
    fun insertVbstIn(entry: VBSTIN) = viewModelScope.launch(Dispatchers.IO) {
        val newId = repository.insertVbstIn(entry)
        val savedEntry = entry.copy(id = newId.toInt())
        _lastInsertedVbstIn.postValue(savedEntry)
        loadVbstIn()
    }

    fun loadVbstIn() = viewModelScope.launch(Dispatchers.IO) {
        val entries = repository.getAllVbstIn()
        _vbstInEntries.postValue(entries)
    }

    fun deleteVbstIn(entry: VBSTIN) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteVbstIn(entry.id)
        loadVbstIn()
    }

    fun deleteAllVbstIn() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAllVbstIn()
        repository.resetVbstInSequence()
        loadVbstIn()
    }

    // VBSTOUT operations
    fun insertVbstOut(entry: VBSTOUT) = viewModelScope.launch(Dispatchers.IO) {
        val newId = repository.insertVbstOut(entry)
        val savedEntry = entry.copy(id = newId.toInt())
        _lastInsertedVbstOut.postValue(savedEntry)
        loadVbstOut()
    }

    fun loadVbstOut() = viewModelScope.launch(Dispatchers.IO) {
        val entries = repository.getAllVbstOut()
        _vbstOutEntries.postValue(entries)
    }

    fun deleteVbstOut(entry: VBSTOUT) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteVbstOut(entry.id)
        loadVbstOut()
    }

    fun deleteAllVbstOut() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAllVbstOut()
        repository.resetVbstOutSequence()
        loadVbstOut()
    }

    // Last inserted entries for undo functionality
    private val _lastInsertedVbstIn = MutableLiveData<VBSTIN>()
    val lastInsertedVbstIn: LiveData<VBSTIN> get() = _lastInsertedVbstIn

    private val _lastInsertedVbstOut = MutableLiveData<VBSTOUT>()
    val lastInsertedVbstOut: LiveData<VBSTOUT> get() = _lastInsertedVbstOut
}