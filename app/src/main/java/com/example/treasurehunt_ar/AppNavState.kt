package com.example.treasurehunt_ar

import androidx.compose.runtime.Stable
import androidx.navigation.NavHostController

@Stable
class AppNavState(
    val navController: NavHostController,
    /* private val snackbarHostState: SnackbarHostState,
    private val snackbarManager: SnackbarManager,
    coroutineScope: CoroutineScope */
) {
    init {
        /* coroutineScope.launch {
            snackbarManager.snackbarMessages.filterNotNull().collect { message ->
                snackbarHostState.showSnackbar(message.asString())
                snackbarManager.clearSnackbarState()
            }
        } */
    }

    fun popUp() {
        navController.navigateUp()
        // navController.popBackStack() // has a bug when pressing multiple times
    }

    fun navigate(route: Route) {
        navController.navigate(route) { launchSingleTop = true }
    }

    fun navigateAndPopUp(route: Route, popUp: Any) {
        navController.navigate(route) {
            launchSingleTop = true
            popUpTo(popUp) { inclusive = true }
        }
    }

    fun clearAndNavigate(route: Route) {
        navController.navigate(route) {
            launchSingleTop = true
            popUpTo(0) { inclusive = true }
        }
    }
}

