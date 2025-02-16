package com.example.treasurehunt_ar.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.treasurehunt_ar.Route
import com.example.treasurehunt_ar.model.AnchorData
import com.example.treasurehunt_ar.model.Game
import com.example.treasurehunt_ar.model.GameState
import com.example.treasurehunt_ar.model.User
import com.example.treasurehunt_ar.model.service.module.ServiceModule
import com.example.treasurehunt_ar.ui.utils.AppViewModel
import com.example.treasurehunt_ar.ui.utils.SnackbarEvent
import com.example.treasurehunt_ar.ui.utils.SnackbarManager
import com.example.treasurehunt_ar.ui.utils.UiText
import com.google.ar.core.Anchor
import com.google.ar.core.Session
import io.github.sceneview.node.Node
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch


class GameViewModel(
    private val serviceModule: ServiceModule
) : AppViewModel() {

    data class GameUiState(
        val localAnchors: List<AnchorData> = emptyList(),
        val resolvedOpponentAnchors: List<Pair<Anchor, AnchorData>> = emptyList(), //(cloudAnchorId, AnchorData)
        val unresolvedAnchors: List<AnchorData> = emptyList(),
        val isHosting: Boolean = false,
        val canAddOpponentAnchors: Boolean = false,
        val showRetryButton: Boolean = false
    )

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Stato del gioco aggiornato in tempo reale (dal db)
    private val _game = MutableStateFlow<Game>(Game())
    val game: StateFlow<Game> = _game.asStateFlow()

    var arSession: Session? = null
        private set
    fun updateArSession(session: Session) {
        arSession = session
    }

    lateinit var childNodes: MutableList<Node>

    var showExitConfirmation by mutableStateOf(false)
        private set


    private var isInitialized = false
    fun initialize(restartApp: (Route) -> Unit) {
        if (isInitialized) return
        isInitialized = true
        launchCatching {
            serviceModule.accountService.currentUser.collect { user ->
                if (user == null) restartApp(Route.Splash)
            }
        }
        observeGame()
    }

    private fun observeGame() {
        launchCatching {
            serviceModule.gamingService.currentGame.filterNotNull().collect { updatedGame  ->
                _game.value = updatedGame
                // Se lo stato è STARTED (posizionamento) e entrambi hanno confermato, avvia hunting phase
                if (_game.value.state == GameState.STARTED && bothAnchorsConfirmed()) {
                    startHuntingPhase()
                }
                if (_game.value.creatorOpponentResolved && _game.value.joinerOpponentResolved) {
                    _uiState.value = _uiState.value.copy(canAddOpponentAnchors = true)
                }
            }
        }
    }

    private fun bothAnchorsConfirmed(): Boolean {
        val g = _game.value
        return g.confirmedAnchorsCreator && g.confirmedAnchorsJoiner
    }

    // Start hunting phase
    private fun startHuntingPhase() {
        launchCatching {
            serviceModule.gamingService.updateGameField("state", GameState.HUNTING)
            serviceModule.gamingService.resolveOpponentAnchors(
                arSession = arSession!!,
                null,
                onAnchorResolved = { resolvedAnchor, anchorData ->
                    val newPair = resolvedAnchor to anchorData
                    _uiState.value = _uiState.value.copy(resolvedOpponentAnchors = _uiState.value.resolvedOpponentAnchors + newPair)
                    if (_uiState.value.resolvedOpponentAnchors.size == REQUIRED_ANCHORS_COUNT)
                        onAllOpponentAnchorsResolved()
                },
                onTimeout = { unresolved ->
                    _uiState.value = _uiState.value.copy(
                        unresolvedAnchors = unresolved,
                        showRetryButton = true
                    )
                })
        }
    }

    fun retryResolveOpponentAnchors() {
        launchCatching {
            _uiState.value = _uiState.value.copy(showRetryButton = false)
            val anchorsToRetry = _uiState.value.unresolvedAnchors
            if (anchorsToRetry.isNotEmpty()) {
                serviceModule.gamingService.resolveOpponentAnchors(
                    arSession = arSession!!,
                    anchorsToResolve = anchorsToRetry,
                    onAnchorResolved = { resolvedAnchor, anchorData ->
                        val newPair = resolvedAnchor to anchorData
                        _uiState.value = _uiState.value.copy(resolvedOpponentAnchors = _uiState.value.resolvedOpponentAnchors + newPair)
                    },
                    onTimeout = { stillUnresolved ->
                        _uiState.value = _uiState.value.copy(
                            unresolvedAnchors = stillUnresolved,
                            showRetryButton = true
                        )
                    }
                )
            } else {
                throw Exception("No anchors in unresolvedAnchors list")
            }
        }
    }

    private fun onAllOpponentAnchorsResolved() {
        launchCatching {
            serviceModule.gamingService.updateOpponentResolvedFlag(userIsCreator(), true)
        }
    }

    fun userIsCreator(): Boolean {
        val currentUserId = serviceModule.accountService.currentUserId
        return _game.value.creator?.uid == currentUserId
    }

    fun getUserProfile(): User {
        return serviceModule.accountService.getUserProfile()
    }

    fun onAnchorPlaced(
        anchor: Anchor,
        model: String,
        onSuccess: (Anchor) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        launchCatching {
            _uiState.value = _uiState.value.copy(isHosting = true)
            try {
                val cloudAnchorId = serviceModule.gamingService.hostCloudAnchor(arSession!!, anchor)
                val anchorData = AnchorData(
                    cloudAnchorId = cloudAnchorId,
                    model = model,
                    position = anchor.pose.translation.toList(),
                    rotation = anchor.pose.rotationQuaternion.toList()
                )
                _uiState.value = _uiState.value.copy(
                    localAnchors = _uiState.value.localAnchors + anchorData
                )
                addAnchorInGame(anchorData)
                onSuccess(anchor)
                viewModelScope.launch {
                    SnackbarManager.sendEvent(
                        event = SnackbarEvent(
                            message = UiText.DynamicString("Object ${_uiState.value.localAnchors.size} hosted successfully")
                        )
                    )
                }
            } catch (e: Exception) {
                onFailure(e)
                throw e
            } finally {
                _uiState.value = _uiState.value.copy(isHosting = false)
            }
        }
    }

    private suspend fun addAnchorInGame(anchorData: AnchorData) {
        val field = if (userIsCreator()) "anchorsCreator" else "anchorsJoiner"
        serviceModule.gamingService.addAnchorToGameField(field, anchorData)
    }

    // Elimina gli anchor locali e aggiorna il record su Firebase
    private fun clearAnchors() {
        _uiState.value = _uiState.value.copy(localAnchors = emptyList())

        if (::childNodes.isInitialized) {
            childNodes.clear()
        }
        // childNodes.value.clear()
    }

    // Aggiorna il flag di conferma nel record Game. Se entrambi hanno confermato, la hunting phase parte (da observeGame).
    fun confirmAnchors() {
        launchCatching {
            // if (_localAnchors.size == REQUIRED_ANCHORS_COUNT) {
            if (_uiState.value.localAnchors.size == REQUIRED_ANCHORS_COUNT) {
                serviceModule.gamingService.updateConfirmedAnchorField(userIsCreator(), true)
                // clearAnchors(childNodes)
                clearAnchors()
            }
        }
    }

    // In fase Hunting: quando l'utente tocca un anchor per confermare di averlo trovato
    fun onAnchorFound(cloudAnchorId: String, onComplete: () -> Unit) {
        launchCatching {
            serviceModule.gamingService.markAnchorFound(cloudAnchorId)
            val allFound = serviceModule.gamingService.checkAllAnchorsFound()
            if (allFound) {
                val success = serviceModule.gamingService.claimWin()
                if (success) {
                    serviceModule.gamingService.updateGameField("state", GameState.ENDED)
                }
            }
            onComplete()
        }
    }

    fun onBackPressed() {
        showExitConfirmation = true
    }

    fun dismissExitConfirmation() {
        showExitConfirmation = false
    }

    fun endGameAndExit(popUpScreen: () -> Unit) {
        launchCatching {
            serviceModule.gamingService.endGame()
            popUpScreen()
        }
    }

    fun exitGame(popUpScreen: () -> Unit) {
        popUpScreen()
    }

    companion object {
        const val REQUIRED_ANCHORS_COUNT = 1
    }

}