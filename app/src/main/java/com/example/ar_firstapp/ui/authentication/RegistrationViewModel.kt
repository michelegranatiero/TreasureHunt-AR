package com.example.ar_firstapp.ui.authentication

import com.example.ar_firstapp.Route
import com.example.ar_firstapp.model.service.AccountService
import com.example.ar_firstapp.ui.utils.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class RegistrationViewModel(
    private val accountService: AccountService
) : AppViewModel() {
    val email = MutableStateFlow("")
    val password = MutableStateFlow("")
    val confirmPassword = MutableStateFlow("")

    fun updateEmail(newEmail: String) {
        email.value = newEmail
    }

    fun updatePassword(newPassword: String) {
        password.value = newPassword
    }

    fun updateConfirmPassword(newConfirmPassword: String) {
        confirmPassword.value = newConfirmPassword
    }

    fun onSignUpClick(openAndPopUp: (Route, Route) -> Unit) {
        launchCatching {
            if (password.value != confirmPassword.value) {
                throw Exception("Passwords do not match")
            }

            accountService.signUp(email.value, password.value)
            openAndPopUp(Route.GameGraph.Home, Route.AuthenticationGraph.Registration)
        }
    }
}