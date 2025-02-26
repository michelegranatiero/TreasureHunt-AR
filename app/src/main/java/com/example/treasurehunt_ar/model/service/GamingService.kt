package com.example.treasurehunt_ar.model.service

import com.example.treasurehunt_ar.model.AnchorData
import com.example.treasurehunt_ar.model.Game
import com.google.ar.core.Anchor
import com.google.ar.core.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GamingService {
    val roomCode: StateFlow<String>
    val currentGame: Flow<Game?>
    suspend fun setRoomCode(roomCode: String)
    suspend fun createRoom()
    suspend fun joinRoom()
    suspend fun leaveRoom()
    suspend fun startGame()
    suspend fun endGame()
    fun onExitGame()

    // AR part:
    suspend fun hostCloudAnchor(arSession: Session, anchor: Anchor): String
    suspend fun resolveOpponentAnchors(
        arSession: Session,
        anchorsToResolve: List<AnchorData>?,
        onAnchorResolved: (Anchor, AnchorData) -> Unit,
        onTimeout: (List<AnchorData>) -> Unit
    )
    suspend fun markAnchorFound(cloudAnchorId: String)
    suspend fun checkAllAnchorsFound(): Boolean
    suspend fun updateGameField(field: String, value: Any)
    suspend fun addAnchorToGameField(field: String, anchorData: AnchorData)
    suspend fun updateConfirmedAnchorField(isCreator: Boolean, confirmed: Boolean)
    suspend fun updateOpponentResolvedFlag(isCreator: Boolean, resolved: Boolean)
    suspend fun claimWin(): Boolean
}