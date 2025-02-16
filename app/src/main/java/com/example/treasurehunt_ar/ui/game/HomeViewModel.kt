package com.example.treasurehunt_ar.ui.game

import com.example.treasurehunt_ar.MatchmakingMode
import com.example.treasurehunt_ar.Route
import com.example.treasurehunt_ar.model.service.AccountService
import com.example.treasurehunt_ar.ui.utils.AppViewModel

class HomeViewModel (
    private val accountService: AccountService
): AppViewModel() {

    fun initialize(restartApp: (Route) -> Unit) {
        launchCatching {
            accountService.currentUser.collect { user ->
                if (user == null) restartApp(Route.Splash)
            }
        }
    }

    fun createGame(openScreen: (Route) -> Unit) {
        openScreen(Route.GameGraph.Matchmaking(MatchmakingMode.CREATE))
    }

    fun joinGame(openScreen: (Route) -> Unit) {
        openScreen(Route.GameGraph.Matchmaking(MatchmakingMode.JOIN))
    }
}