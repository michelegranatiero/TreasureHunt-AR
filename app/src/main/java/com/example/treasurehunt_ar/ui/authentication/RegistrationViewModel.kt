package com.example.treasurehunt_ar.ui.authentication

import com.example.treasurehunt_ar.Route
import com.example.treasurehunt_ar.model.service.AccountService
import com.example.treasurehunt_ar.ui.utils.AppViewModel
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
            if (!email.value.isValidEmail()) {
                throw IllegalArgumentException("Invalid email format")
            }

            if (!password.value.isValidPassword()) {
                throw IllegalArgumentException("Invalid password format")
            }

            if (password.value != confirmPassword.value) {
                throw IllegalArgumentException("Passwords do not match")
            }

            // IF SIGN UP FROM SCRATCH
            // accountService.signUpWithEmail(email.value, password.value)
            // ELSE IF ALREADY CREATED ANONYMOUS ACCOUNT
            accountService.linkAccountWithEmail(email.value, password.value)

            openAndPopUp(Route.GameGraph.Home, Route.AuthenticationGraph.Registration)
        }
    }


    // ONLY IF USER HAS ANONYMOUS ACCOUNT AT STARTUP (link it with google)
    /* fun onSignUpWithGoogle(credential: Credential, openAndPopUp: (Route, Route) -> Unit) {
        launchCatching {
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                accountService.linkAccountWithGoogle(googleIdTokenCredential.idToken)
                openAndPopUp(Route.GameGraph.Home, Route.AuthenticationGraph.Registration)
            } else {
                Log.e(ERROR_TAG, UNEXPECTED_CREDENTIAL)
            }
        }
    } */

    fun onSignInClick(openAndPopUp: (Route, Route) -> Unit) {
        openAndPopUp(Route.AuthenticationGraph.Login, Route.AuthenticationGraph.Registration)
    }
}