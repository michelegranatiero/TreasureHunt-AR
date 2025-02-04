package com.example.treasurehunt_ar.ui.authentication

import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.example.treasurehunt_ar.R
import com.example.treasurehunt_ar.Route
import com.example.treasurehunt_ar.model.service.AccountService
import com.example.treasurehunt_ar.ui.utils.AppViewModel
import com.example.treasurehunt_ar.ui.utils.SnackbarEvent
import com.example.treasurehunt_ar.ui.utils.SnackbarManager
import com.example.treasurehunt_ar.ui.utils.UNEXPECTED_CREDENTIAL
import com.example.treasurehunt_ar.ui.utils.UiText
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
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
            accountService.signInWithEmail(email.value, password.value)
            openAndPopUp(Route.GameGraph.Home, Route.AuthenticationGraph.Login)
        }
    }

    fun onSignUpClick(openAndPopUp: (Route, Route) -> Unit) {
        openAndPopUp(Route.AuthenticationGraph.Registration, Route.AuthenticationGraph.Login)
    }

    fun onSignInWithGoogle(credential: Credential, openAndPopUp: (Route, Route) -> Unit) {
        launchCatching {
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                // accountService.signInWithGoogle(googleIdTokenCredential.idToken)
                accountService.linkAccountWithGoogle(googleIdTokenCredential.idToken)

                SnackbarManager.sendEvent(event = SnackbarEvent(
                    message = UiText.StringResource(R.string.sign_in_success)
                ))

                openAndPopUp(Route.GameGraph.Home, Route.AuthenticationGraph.Login)
            } else {
                Log.e(ERROR_TAG, UNEXPECTED_CREDENTIAL)
            }
        }
    }

    fun onSignInAnonymous(openAndPopUp: (Route, Route) -> Unit) {
        launchCatching {
            accountService.createAnonymousAccount()
            openAndPopUp(Route.GameGraph.Home, Route.AuthenticationGraph.Login)
        }
    }



}