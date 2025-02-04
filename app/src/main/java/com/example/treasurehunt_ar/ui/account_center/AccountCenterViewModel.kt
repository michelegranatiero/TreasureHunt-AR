package com.example.treasurehunt_ar.ui.account_center

import com.example.treasurehunt_ar.Route
import com.example.treasurehunt_ar.model.User
import com.example.treasurehunt_ar.model.service.AccountService
import com.example.treasurehunt_ar.ui.utils.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountCenterViewModel(
    private val accountService: AccountService
): AppViewModel() {
    private val _user = MutableStateFlow(User())
    val user: StateFlow<User> = _user.asStateFlow()

    init {
        launchCatching {
            _user.value = accountService.getUserProfile()
        }
    }

    fun onUpdateDisplayNameClick(newDisplayName: String) {
        launchCatching {
            accountService.updateDisplayName(newDisplayName)
            _user.value = accountService.getUserProfile()
        }
    }

    fun onSignInClick(openScreen: (Route) -> Unit) = openScreen(Route.AuthenticationGraph.Login)

    fun onSignOutClick(restartApp: (Route) -> Unit) {
        launchCatching {
            accountService.signOut()
            restartApp(Route.Splash)
        }
    }

    fun onDeleteAccountClick(restartApp: (Route) -> Unit) {
        launchCatching {
            accountService.deleteAccount()
            restartApp(Route.Splash)
        }
    }
}