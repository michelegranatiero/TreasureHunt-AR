package com.example.treasurehunt_ar.ui.game

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.treasurehunt_ar.MatchmakingMode
import com.example.treasurehunt_ar.Route
import com.example.treasurehunt_ar.model.Game
import com.example.treasurehunt_ar.model.GameState
import com.example.treasurehunt_ar.model.service.module.ServiceModule
import com.example.treasurehunt_ar.ui.utils.AppViewModel
import com.example.treasurehunt_ar.ui.utils.SnackbarEvent
import com.example.treasurehunt_ar.ui.utils.SnackbarManager
import com.example.treasurehunt_ar.ui.utils.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class MatchmakingUiState(
    val matchmakingMode: MatchmakingMode,
    val roomCode: String = "",
    val joiningRoom: Boolean = false,
    val game: Game? = null
)

class MatchmakingViewModel(
    private val serviceModule: ServiceModule,
    handle: SavedStateHandle
) : AppViewModel() {

    private val mode = handle.toRoute<Route.GameGraph.Matchmaking>().mode

    private val _uiState = MutableStateFlow(MatchmakingUiState(matchmakingMode = mode))
    val uiState: StateFlow<MatchmakingUiState> = _uiState.asStateFlow()

    private var gameObservationJob: Job? = null

    private lateinit var navigateAndPopUp: (Route, Route) -> Unit

    private var isInitialized = false
    fun initialize(restartApp: (Route) -> Unit, openAndPopUp: (Route, Route) -> Unit) {
        navigateAndPopUp = openAndPopUp //on every recomposition, otherwise crash

        if (isInitialized) return
        isInitialized = true
        launchCatching {
            serviceModule.accountService.currentUser.collect { user ->
                if (user == null) restartApp(Route.Splash)
            }
        }
        if (mode == MatchmakingMode.CREATE) createRoom()
    }

    fun updateRoomCode(code: String) {
        _uiState.value = _uiState.value.copy(roomCode = code)
    }

    fun setJoiningRoomFlag(joining: Boolean) {
        _uiState.value = _uiState.value.copy(joiningRoom = joining)
    }

    private fun createRoom() {
        launchCatching {
            serviceModule.gamingService.createRoom()
            _uiState.value = uiState.value.copy(roomCode = serviceModule.gamingService.roomCode.value)
            startObservingGame()
        }
    }

    fun joinRoom() {
        val code = uiState.value.roomCode
        if (code.isBlank()) {
            viewModelScope.launch {
                SnackbarManager.sendEvent(event = SnackbarEvent(
                    message = UiText.DynamicString("Empty Room Code!"),
                ))
            }
        }else{
            launchCatching {
                serviceModule.gamingService.setRoomCode(code)
                serviceModule.gamingService.joinRoom()
                startObservingGame()
            }
            setJoiningRoomFlag(false)
        }

    }

    fun handleQRScanResult(qrCode: String) {
        launchCatching {
            updateRoomCode(qrCode)
            joinRoom()
            setJoiningRoomFlag(true)
        }
    }

    fun startGame() {
        launchCatching {
            serviceModule.gamingService.startGame() // also need to initialize things ......
        }
    }

    private fun startObservingGame() {
        gameObservationJob?.cancel()
        gameObservationJob = launchCatching {
            serviceModule.gamingService.currentGame
                .catch { e ->
                    //if room is cancelled, throw error
                    if (e.message?.contains("Room cancelled") == true) {
                        handleRoomCancelled()
                    }
                    throw e
                }
                .collect { game ->
                _uiState.value = _uiState.value.copy(game = game)
                if (game?.state == GameState.STARTED) {
                    navigateToGameScreen()
                }
            }
        }
    }

    private fun navigateToGameScreen() {
        navigateAndPopUp(Route.GameGraph.Game, Route.GameGraph.Matchmaking(mode))
    }

    private fun handleRoomCancelled() {
        gameObservationJob?.cancel()
        _uiState.value = MatchmakingUiState(matchmakingMode = mode)
        if (mode == MatchmakingMode.CREATE) {
            createRoom()
        }
    }

    fun exitGame(popUpScreen: () -> Unit) {
        gameObservationJob?.cancel()
        launchCatching {
            serviceModule.gamingService.leaveRoom()
        }
        popUpScreen()
    }

    // only for joiner before joining a room
    fun exitMatchmaking(popUpScreen: () -> Unit) {
        popUpScreen()
    }

    //only for joiner after joining a room
    fun exitGameAndResetViewModel() {
        gameObservationJob?.cancel()
        launchCatching {
            serviceModule.gamingService.leaveRoom()
        }
        _uiState.value = MatchmakingUiState(matchmakingMode = mode, roomCode = uiState.value.roomCode)
    }
}