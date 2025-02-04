package com.example.treasurehunt_ar.ui.utils

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SnackbarEvent(
    val message: UiText,
    val action: SnackBarAction? = null
)

data class SnackBarAction(
    val name: String,
    val action: () -> Unit
)

object SnackbarManager {

    /* private val messages: MutableStateFlow<UiText?> = MutableStateFlow(null)
    val snackbarMessages: StateFlow<UiText?>
        get() = messages

    fun showMessage(message: UiText) {
        messages.value = message
    }

    fun clearSnackbarState() {
        messages.value = null
    } */

    private val _events = Channel<SnackbarEvent>()
    val events = _events.receiveAsFlow()

    suspend fun sendEvent(event: SnackbarEvent) {
        _events.send(event)
    }


}

@Composable
fun SnackbarFlowHelper(
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
){
    ObserveAsEvents(
        flow = SnackbarManager.events,
        snackbarHostState
    ) { event ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss() // Dismiss current snackbar
            val result = snackbarHostState.showSnackbar(
                message = event.message.asString(context),
                actionLabel = event.action?.name,
                // withDismissAction = true,
                duration = SnackbarDuration.Long
            )

            when (result) {
                SnackbarResult.ActionPerformed -> {
                    event.action?.action?.invoke()
                }
                SnackbarResult.Dismissed -> {}
            }
        }
    }

}


@Composable
fun <T> ObserveAsEvents(flow: Flow<T>, key: Any? = null, onEvent: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect (lifecycleOwner.lifecycle, key, flow) {
        //collect flows in a lifecycle-aware way
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate){
                flow.collect(onEvent)
            }
        }
    }
}

class SnackbarViewModel : ViewModel() {
    val snackbarHostState = SnackbarHostState()
}