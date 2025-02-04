package com.example.treasurehunt_ar.ui.utils

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

open class AppViewModel : ViewModel() {
    //to launch a coroutine and catch exceptions without blocking the main thread
    fun launchCatching(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                Log.d(ERROR_TAG, throwable.message.orEmpty())
                viewModelScope.launch {
                    SnackbarManager.sendEvent(event = SnackbarEvent(
                        message = UiText.DynamicString("Error: ${throwable.message.orEmpty()}"),
                    ))
                }
            },
            block = block
        )

    companion object {
        const val ERROR_TAG = "ARTREASUREHUNT APP ERROR"
    }
}