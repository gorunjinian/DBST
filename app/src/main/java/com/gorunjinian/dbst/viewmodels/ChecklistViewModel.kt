package com.gorunjinian.dbst.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorunjinian.dbst.data.ChecklistItem
import com.gorunjinian.dbst.data.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ChecklistViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uncheckedItems = MutableLiveData<List<ChecklistItem>>()
    private val uncheckedItems: LiveData<List<ChecklistItem>> get() = _uncheckedItems

    private val _checkedItems = MutableLiveData<List<ChecklistItem>>()
    private val checkedItems: LiveData<List<ChecklistItem>> get() = _checkedItems

    private val _checkedItemsCount = MutableLiveData<Int>()
    private val checkedItemsCount: LiveData<Int> get() = _checkedItemsCount

    init {
        loadItems()
    }

    fun loadItems(): Triple<List<ChecklistItem>, List<ChecklistItem>, Int> {
        val scope = viewModelScope.launch(Dispatchers.IO) {
            val unchecked = repository.getUncheckedItems()
            val checked = repository.getCheckedItems()
            val count = repository.getCheckedItemsCount()

            _uncheckedItems.postValue(unchecked)
            _checkedItems.postValue(checked)
            _checkedItemsCount.postValue(count)
        }

        // Return the current LiveData values
        // Note: If this is the first load, they might still be null/empty
        return Triple(
            uncheckedItems.value ?: emptyList(),
            checkedItems.value ?: emptyList(),
            checkedItemsCount.value ?: 0
        )
    }

    fun addItem(text: String) = viewModelScope.launch(Dispatchers.IO) {
        // Get the max position to insert the new item at the top
        val maxPosition = repository.getUncheckedItems().maxByOrNull { it.position }?.position ?: 0
        val newItem = ChecklistItem(
            text = text,
            isChecked = false,
            position = maxPosition + 1
        )
        repository.insertChecklistItem(newItem)
        loadItems()
    }

    fun toggleItemChecked(item: ChecklistItem) = viewModelScope.launch(Dispatchers.IO) {
        val updatedItem = item.copy(isChecked = !item.isChecked)
        repository.updateChecklistItem(updatedItem)
        loadItems()
    }

    fun deleteItem(item: ChecklistItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteChecklistItem(item.id)
        loadItems()
    }

    fun reorderItems(items: List<ChecklistItem>) = viewModelScope.launch(Dispatchers.IO) {
        items.forEachIndexed { index, item ->
            if (item.position != index) {
                repository.updateChecklistItemPosition(item.id, index)
            }
        }
        loadItems()
    }
}