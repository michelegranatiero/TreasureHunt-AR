package com.example.ar_firstapp.ui.authentication

import com.example.ar_firstapp.Route
import com.example.ar_firstapp.model.service.AccountService
import com.example.ar_firstapp.ui.utils.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class LoginViewModel(
    private val accountService: AccountService
) : AppViewModel() {
    val email = MutableStateFlow("")
    val password = MutableStateFlow("")

    fun updateEmail(newEmail: String) {
        email.value = newEmail
    }

    fun updatePassword(newPassword: String) {
        password.value = newPassword
    }

    fun onSignInClick(openAndPopUp: (Route, Route) -> Unit) {
        launchCatching {
            accountService.signIn(email.value, password.value)
            openAndPopUp(Route.GameGraph.Home, Route.AuthenticationGraph.Login)
        }
    }

    fun onSignUpClick(openAndPopUp: (Route, Route) -> Unit) {
        openAndPopUp(Route.AuthenticationGraph.Registration, Route.AuthenticationGraph.Login)

    }



}