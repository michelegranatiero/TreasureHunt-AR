package com.example.treasurehunt_ar.model.service.impl

import android.util.Log
import com.example.treasurehunt_ar.model.AnchorData
import com.example.treasurehunt_ar.model.Game
import com.example.treasurehunt_ar.model.GameState
import com.example.treasurehunt_ar.model.PlayerData
import com.example.treasurehunt_ar.model.service.AccountService
import com.example.treasurehunt_ar.model.service.GamingService
import com.google.ar.core.Anchor
import com.google.ar.core.Session
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GamingServiceImpl(private val auth: AccountService) : GamingService {

    private val database = Firebase.database.reference

    private val _roomCode = MutableStateFlow("")
    override val roomCode: StateFlow<String> = _roomCode.asStateFlow()

    override suspend fun setRoomCode(roomCode: String) {
        _roomCode.value = roomCode
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentGame: Flow<Game?> = _roomCode
        .filterNotNull()
        .filter { it.isNotEmpty() }
        .flatMapLatest { code ->
            callbackFlow {
                val ref = database.child("rooms").child(code)
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            close(Exception("Room cancelled"))
                            return
                        }
                        val game = snapshot.getValue(Game::class.java)
                        trySend(game).isSuccess
                    }
                    override fun onCancelled(error: DatabaseError) {
                        close(error.toException())
                    }
                }
                ref.addValueEventListener(listener)
                awaitClose { ref.removeEventListener(listener) }
            }
        }

    override suspend fun createRoom() {
        val ref = database.child("rooms")
        val roomRef = ref.push()
        val roomCode = roomRef.key ?: throw Exception("Failed to create room")
        val game = Game(
            creator = PlayerData(
                displayName = auth.getUserProfile().displayName,
                uid = auth.currentUserId
            ),
            state = GameState.OPEN,
        )
        roomRef.setValue(game).await()
        _roomCode.value = roomCode

        setupPresence()
    }


    override suspend fun joinRoom() {
        val ref = database.child("rooms").child(_roomCode.value)
        val snapshot = ref.get().await()
        if (!snapshot.exists()) {
            throw Exception("Room not found")
        }
        suspendCancellableCoroutine { cont ->
            var errorMessage: String? = null
            ref.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val game = mutableData.getValue(Game::class.java) ?: return Transaction.success(
                        mutableData
                    )
                    if (game.creator?.uid == auth.currentUserId){
                        errorMessage = "Cannot join your own room"
                        return Transaction.abort()
                    }else if (game.state != GameState.OPEN) {
                        errorMessage = "Room is full"
                        return Transaction.abort()
                    }
                    game.joiner = PlayerData(
                        displayName = auth.getUserProfile().displayName,
                        uid = auth.currentUserId
                    )
                    game.state = GameState.JOINED
                    mutableData.value = game

                    return Transaction.success(mutableData)
                }
                override fun onComplete(
                    databaseError: DatabaseError?,
                    committed: Boolean,
                    snapshot: DataSnapshot?
                ) {
                    when {
                        databaseError != null ->
                            cont.resumeWithException(databaseError.toException())

                        !committed ->
                            cont.resumeWithException(
                                Exception(
                                    errorMessage ?: "Transaction aborted"
                                )
                            )

                        else -> {
                            cont.resume(Unit)
                        }
                    }
                }
            })
        }
        setupPresence()
    }

    override suspend fun leaveRoom() {
        val ref = database.child("rooms").child(_roomCode.value)
        suspendCancellableCoroutine { cont ->
            ref.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val game = mutableData.getValue(Game::class.java)
                        ?: return Transaction.success(mutableData) // se non esiste, consideriamo l'operazione completata
                    if (game.creator?.uid == auth.currentUserId) {
                        // Se il creator abbandona la room, rimuoviamo la room
                        mutableData.value = null
                    } else if (game.joiner?.uid == auth.currentUserId) {
                        // Se il joiner abbandona la room, lo rimuoviamo e riportiamo il game allo stato OPEN
                        game.joiner = null
                        game.state = GameState.OPEN
                        mutableData.value = game
                    }
                    return Transaction.success(mutableData)
                }
                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    snapshot: DataSnapshot?
                ) {
                    when {
                        error != null ->
                            cont.resumeWithException(error.toException())
                        !committed ->
                            cont.resumeWithException(Exception("Leave room transaction aborted"))
                        else ->
                            cont.resume(Unit)
                    }
                }
            })
        }
        _roomCode.value = ""
    }

    override suspend fun startGame() {
        val ref = database.child("rooms").child(_roomCode.value)
        val snapshot = ref.get().await()
        if (!snapshot.exists()) throw Exception("Room not found")
        val game = snapshot.getValue(Game::class.java)
        if (game != null) {
            val updates = mapOf("state" to GameState.STARTED.name)
            ref.updateChildren(updates).await()
        } else {
            throw Exception("Game data missing")
        }
    }


    private var presenceListener: ValueEventListener? = null

    private fun setupPresence() {
        val roomCode = _roomCode.value
        if (roomCode.isEmpty()) return

        val userId = auth.currentUserId
        val roomRef = database.child("rooms").child(roomCode)

        presenceListener?.let { roomRef.removeEventListener(it) }

        // Registra un listener persistente per monitorare il nodo della room
        presenceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // room does not exist anymore: cancel onDisconnect operations and remove listener
                    //this is because otherwise the onDisconnect operations will be executed and the room recreated with setValue
                    roomRef.onDisconnect().cancel()
                    roomRef.removeEventListener(this)
                    presenceListener = null
                    return
                }
                val game = snapshot.getValue(Game::class.java) ?: return

                when (game.state) {
                    GameState.OPEN, GameState.JOINED -> {
                        when (userId) {
                            game.creator?.uid -> {
                                roomRef.onDisconnect().removeValue()
                            }
                            game.joiner?.uid -> {
                                roomRef.child("joiner").onDisconnect().removeValue()
                                roomRef.child("state").onDisconnect().setValue(GameState.OPEN)
                            }
                        }
                    }
                    // if game STARTED, cancel onDisconnect operations and remove listener
                    GameState.STARTED, GameState.HUNTING -> {
                        roomRef.onDisconnect().cancel() // Cancella altri onDisconnect precedenti
                        roomRef.child("state").onDisconnect().setValue(GameState.ENDED)
                        roomRef.removeEventListener(this)
                        presenceListener = null
                    }
                    GameState.ENDED -> {
                        roomRef.onDisconnect().cancel()
                        roomRef.removeEventListener(this)
                        presenceListener = null
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                throw error.toException()
            }
        }

        roomRef.addValueEventListener(presenceListener!!)
    }

    override suspend fun endGame() {
        val ref = database.child("rooms").child(_roomCode.value)
        val snapshot = ref.get().await()
        if (!snapshot.exists()) {
            throw Exception("Room not found")
        }
        val game = snapshot.getValue(Game::class.java)
        if (game != null) {
            game.state = GameState.ENDED
            ref.setValue(game).await()
        } else {
            throw Exception("Game data missing")
        }
    }

    private fun cleanupPresence() {
        val roomCode = _roomCode.value
        if (roomCode.isEmpty()) return
        val roomRef = database.child("rooms").child(roomCode)
        presenceListener?.let {
            roomRef.removeEventListener(it)
            presenceListener = null
        }
        // Cancella eventuali operazioni onDisconnect
        roomRef.onDisconnect().cancel()
    }

    override fun onExitGame() {
        cleanupPresence()
        _roomCode.value = ""
    }


    //AR GAME

    override suspend fun hostCloudAnchor(arSession: Session, anchor: Anchor): String =
        suspendCancellableCoroutine { cont ->
        var resumed = false
        // Avvia l'hosting del cloud anchor, con time to live (1 day)
        arSession.hostCloudAnchorAsync(anchor, 1) { cloudAnchorId, cloudState ->
            if (!resumed) {
                if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
                    resumed = true
                    cont.resume(cloudAnchorId)
                } else if (cloudState.isError) {
                    resumed = true
                    cont.resumeWithException(Exception("Hosting failed: $cloudState"))
                }
            }
        }
    }

    override suspend fun resolveOpponentAnchors(
        arSession: Session,
        anchorsToResolve: List<AnchorData>?,    // Se anchorsToResolve è null, vengono letti dal DB
        onAnchorResolved: (Anchor, AnchorData) -> Unit,
        onTimeout: (List<AnchorData>) -> Unit
    ) {
        // Recupera gli opponent anchors dal DB se non sono stati passati in anchorsToResolve
        val opponentAnchors: List<AnchorData> = anchorsToResolve ?: run {
            val gameSnapshot = database.child("rooms").child(_roomCode.value).get().await()
            val game = gameSnapshot.getValue(Game::class.java) ?: return
            val currentUserId = auth.currentUserId
            if (game.creator?.uid != currentUserId) game.anchorsCreator ?: emptyList() else game.anchorsJoiner ?: emptyList()
        }

        if (opponentAnchors.isEmpty()) return

        // Risolvi in parallelo tutti gli anchor con async/await
        val unresolved: List<AnchorData> = coroutineScope {
            opponentAnchors.map { anchorData ->
                async {
                    try {
                        val resolvedAnchor = withTimeout(30000L) { // timeout for each anchor resolution
                            suspendCancellableCoroutine<Anchor> { cont ->
                                var resumed = false
                                arSession.resolveCloudAnchorAsync(anchorData.cloudAnchorId) { anchor, cloudState ->
                                    if (!resumed) {
                                        when {
                                            cloudState == Anchor.CloudAnchorState.SUCCESS -> {
                                                resumed = true
                                                cont.resume(anchor)
                                            }
                                            cloudState.isError -> {
                                                resumed = true
                                                cont.resumeWithException(Exception("Resolution failed: $cloudState"))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Log.d("GamingService", "Anchor risolto: $anchorData")
                        onAnchorResolved(resolvedAnchor, anchorData)
                        null  // Ritorna null se risolto
                    } catch (e: TimeoutCancellationException) {
                        Log.e("GamingService", "Timeout per ${anchorData.cloudAnchorId}")
                        anchorData  // Ritorna l'anchor non risolto
                    } catch (e: Exception) {
                        Log.e("GamingService", "Errore per ${anchorData.cloudAnchorId}: ${e.message}")
                        anchorData
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (unresolved.isNotEmpty()) {
            onTimeout(unresolved)
        }
    }


    override suspend fun markAnchorFound(cloudAnchorId: String) {
        val roomRef = database.child("rooms").child(_roomCode.value)
        val snapshot = roomRef.get().await()
        val game = snapshot.getValue(Game::class.java) ?: return
        val isCreator = game.creator?.uid == auth.currentUserId

        val playerRef = roomRef.child(if (isCreator) "creator" else "joiner")
        suspendCancellableCoroutine { cont ->
            playerRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val playerData = mutableData.getValue(PlayerData::class.java)
                        ?: return Transaction.success(mutableData)
                    // Incrementa anchorsFound di 1
                    val updated = playerData.copy(anchorsFound = playerData.anchorsFound + 1)
                    mutableData.value = updated
                    return Transaction.success(mutableData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    when {
                        error != null -> cont.resumeWithException(error.toException())
                        !committed -> cont.resumeWithException(Exception("markAnchorFound transaction aborted"))
                        else -> cont.resume(Unit)
                    }
                }
            })
        }
    }

    override suspend fun checkAllAnchorsFound(): Boolean {
        val roomRef = database.child("rooms").child(_roomCode.value)
        val snapshot = roomRef.get().await()
        val game = snapshot.getValue(Game::class.java) ?: return false
        val currentUserId = auth.currentUserId
        val isCreator = game.creator?.uid == currentUserId

        // Se l'utente è creator, controlliamo i dati del joiner, altrimenti quelli del creator.
        val (opponentFoundCount, opponentAnchorsCount) = if (isCreator) {
            //user is creator, found by creator and count by joiner
            val found = game.creator?.anchorsFound ?: 0
            val count = game.anchorsJoiner?.size ?: 0
            count to found
        } else { //user is joiner, found by joiner and count by creator
            val found = game.joiner?.anchorsFound ?: 0
            val count = game.anchorsCreator?.size ?: 0
            count to found
        }
        return opponentFoundCount == opponentAnchorsCount && opponentAnchorsCount > 0
    }

    override suspend fun updateGameField(field: String, value: Any) {
        val ref = database.child("rooms").child(_roomCode.value)
        ref.child(field).setValue(value).await()
    }

    override suspend fun addAnchorToGameField(field: String, anchorData: AnchorData) {
        val ref = database.child("rooms").child(_roomCode.value).child(field)
        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentList = mutableData.getValue(object : GenericTypeIndicator<List<AnchorData>>() {}) ?: emptyList()
                // add new anchor to the list
                val newList = currentList + anchorData
                mutableData.value = newList
                return Transaction.success(mutableData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    throw error.toException()
                } else if (!committed) {
                    throw Exception("Transaction aborted")
                } // else nothing to do
            }
        })
    }

    override suspend fun updateConfirmedAnchorField(isCreator: Boolean, confirmed: Boolean) {
        val ref = database.child("rooms").child(_roomCode.value)
        val field = if (isCreator) "confirmedAnchorsCreator" else "confirmedAnchorsJoiner"
        ref.child(field).setValue(confirmed).await()
    }

    override suspend fun updateOpponentResolvedFlag(isCreator: Boolean, resolved: Boolean) {
        val ref = database.child("rooms").child(_roomCode.value)
        val field = if (isCreator) "creatorOpponentResolved" else "joinerOpponentResolved"
        ref.child(field).setValue(resolved).await()
    }

    override suspend fun claimWin(): Boolean {
        val ref = database.child("rooms").child(_roomCode.value)
        return suspendCancellableCoroutine { cont ->
            ref.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val game = mutableData.getValue(Game::class.java) ?: return Transaction.success(mutableData)
                    if (game.winner != null) return Transaction.abort() // Se già c'è un vincitore, abort
                    game.winner = auth.currentUserId
                    mutableData.value = game

                    return Transaction.success(mutableData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (error != null || !committed) cont.resume(false)
                    else cont.resume(true)
                }
            })
        }
    }
}