package com.gorunjinian.dbst.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gorunjinian.dbst.data.AppRepository

class ViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(EntryViewModel::class.java) ->
                EntryViewModel(repository) as T
            modelClass.isAssignableFrom(ValidityViewModel::class.java) ->
                ValidityViewModel(repository) as T
            modelClass.isAssignableFrom(TetherViewModel::class.java) ->
                TetherViewModel(repository) as T
            modelClass.isAssignableFrom(ChecklistViewModel::class.java) ->
                ChecklistViewModel(repository) as T
            modelClass.isAssignableFrom(GivenValuesViewModel::class.java) ->
                ChecklistViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}