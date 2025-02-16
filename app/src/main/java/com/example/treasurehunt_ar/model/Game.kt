package com.example.treasurehunt_ar.model

data class PlayerData(
    val displayName: String = "",
    val uid: String = "",
    val anchorsFound: Int = 0 // Numero di anchor trovati in fase HUNTING
)

data class Game(
    var state: GameState = GameState.OPEN,
    val creator: PlayerData? = null,
    var joiner: PlayerData? = null,
    var anchorsCreator: List<AnchorData>? = null,
    var anchorsJoiner: List<AnchorData>? = null,
    var confirmedAnchorsCreator: Boolean = false,    // Flag di conferma del posizionamento per il creator
    var confirmedAnchorsJoiner: Boolean = false,        // Flag di conferma del posizionamento per il joiner
    var creatorOpponentResolved: Boolean = false,    // Flag di risoluzione degli anchor dell'avversario per il creator
    var joinerOpponentResolved: Boolean = false,        // Flag di risoluzione degli anchor dell'avversario per il joiner
    var winner: String? = null
)

enum class GameState {
    OPEN,
    JOINED,
    STARTED,
    HUNTING,
    ENDED
}

data class AnchorData(
    val cloudAnchorId: String = "",
    val model: String = "",
    val position: List<Float>? = null,
    val rotation: List<Float>? = null,
    val scale: Float = 1f
)