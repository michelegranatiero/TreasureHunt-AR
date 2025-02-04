package com.example.treasurehunt_ar.ui.splash

import androidx.lifecycle.viewModelScope
import com.example.treasurehunt_ar.Route
import com.example.treasurehunt_ar.model.service.AccountService
import com.example.treasurehunt_ar.ui.utils.AppViewModel
import com.example.treasurehunt_ar.ui.utils.SnackbarEvent
import com.example.treasurehunt_ar.ui.utils.SnackbarManager
import com.example.treasurehunt_ar.ui.utils.UiText
import kotlinx.coroutines.launch

class SplashViewModel (
    private val accountService: AccountService
) : AppViewModel() {

    fun onAppStart(openAndPopUp: (Route, Route) -> Unit) {
        if (accountService.hasUser()) openAndPopUp(Route.GameGraph.Home, Route.Splash)
        else createAnonymousAccount(openAndPopUp)
        // else openAndPopUp(Route.AuthenticationGraph.Login, Route.Splash)

        // SnackbarManager.showMessage(UiText.DynamicString("Welcome to the app!"))
        viewModelScope.launch {
            SnackbarManager.sendEvent(event = SnackbarEvent(
                message = UiText.DynamicString("Welcome to the app!"),
            ))
        }

    }

    private fun createAnonymousAccount(openAndPopUp: (Route, Route) -> Unit) {
        launchCatching {
            accountService.createAnonymousAccount()
            openAndPopUp(Route.GameGraph.Home, Route.Splash)
        }
    }
}