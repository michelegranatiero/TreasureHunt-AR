package com.example.treasurehunt_ar.ui.utils

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/*
fun <VM: ViewModel> customViewModelFactory(initializer: () -> VM): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return initializer() as T
        }
    }
}*/

fun <VM : ViewModel> customViewModelFactory(
    initializer: (SavedStateHandle) -> VM
): ViewModelProvider.Factory {
    return object : AbstractSavedStateViewModelFactory() {
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            return initializer(handle) as T
        }
    }
}