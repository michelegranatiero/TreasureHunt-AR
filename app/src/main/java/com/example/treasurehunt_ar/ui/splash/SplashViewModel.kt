package com.example.treasurehunt_ar.ui.splash

import com.example.treasurehunt_ar.model.service.AccountService
import com.example.treasurehunt_ar.ui.utils.AppViewModel

class SplashViewModel (
    private val accountService: AccountService
) : AppViewModel() {

    fun onAppStart(navigateToMain: () -> Unit) {
        if (accountService.hasUser()) navigateToMain()
        else createAnonymousAccount(navigateToMain)

        /* viewModelScope.launch {
            SnackbarManager.sendEvent(event = SnackbarEvent(
                message = UiText.DynamicString("Welcome to the app!"),
            ))
        } */

    }

    private fun createAnonymousAccount(navigateToMain: () -> Unit) {
        launchCatching {
            accountService.createAnonymousAccount()
            navigateToMain()
        }
    }
}