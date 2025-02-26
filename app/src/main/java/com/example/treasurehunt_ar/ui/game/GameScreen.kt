package com.example.treasurehunt_ar.ui.game

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.treasurehunt_ar.Route
import com.example.treasurehunt_ar.TreasureHuntApplication
import com.example.treasurehunt_ar.model.GameState
import com.example.treasurehunt_ar.ui.utils.components.ExitDialog
import com.example.treasurehunt_ar.ui.utils.customViewModelFactory
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotTrackingException
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.collision.Vector3
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberView
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@Composable
fun GameScreen(
    restartApp: (Route) -> Unit,
    popUpScreen: () -> Unit,
    viewModel: GameViewModel = viewModel<GameViewModel>(
        factory = customViewModelFactory {
            GameViewModel(TreasureHuntApplication.serviceModule)
        }
    )
) {
    LaunchedEffect(Unit) { viewModel.initialize(restartApp) }

    val uiState by viewModel.uiState.collectAsState()
    val tappedAnchors = remember { mutableStateListOf<String>() }
    val game by viewModel.game.collectAsState()
    val currentModel = remember { mutableStateOf("chest5") }

    var mappingQuality by remember { mutableStateOf(Session.FeatureMapQuality.INSUFFICIENT) }

    // Setup AR components
    var frame by remember { mutableStateOf<Frame?>(null) }
    val engine = rememberEngine()
    val view = rememberView(engine)
    val renderer = rememberRenderer(engine)
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberARCameraNode(engine)
    val collisionSystem = rememberCollisionSystem(view)
    var planeRenderer by remember { mutableStateOf(true) }
    var trackingFailureReason by remember { mutableStateOf<TrackingFailureReason?>(null) }

    // val childNodes by viewModel.childNodes.collectAsState()
    val childNodes = remember { mutableStateListOf<Node>() }
    LaunchedEffect(childNodes) { viewModel.childNodes = childNodes }

    // Lista per i nodi degli anchor avversari risolti (da posizionare ma non visibili)
    val opponentNodes = remember { mutableStateListOf<AnchorNode>() }
    LaunchedEffect(uiState.canAddOpponentAnchors) {
        Log.d("GameScreen1", "2 resolvedOpponentAnchors: ${uiState.resolvedOpponentAnchors.size}")
        uiState.resolvedOpponentAnchors.forEach { (resolvedAnchor, anchorData) ->
            if (opponentNodes.none { it.anchor == resolvedAnchor }) {
                val opponentNode = createAnchorNode(
                    engine = engine,
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                    model = anchorData.model,
                    anchor = resolvedAnchor,
                    visible = false // inizialmente non visibile
                )
                opponentNodes.add(opponentNode)
                childNodes.add(opponentNode)
            }
        }
    }

    LaunchedEffect(opponentNodes.size) {
        if (opponentNodes.size == game.numberOfAnchors) {
            while (true) {
                frame?.let { currentFrame ->
                    val cameraPosition = currentFrame.camera.pose.toVector3()
                    childNodes.forEach { node ->
                        (node as? AnchorNode)?.let { anchorNode ->
                            val anchorPosition = anchorNode.worldPosition
                            val dx = cameraPosition.x - anchorPosition.x
                            val dy = cameraPosition.y - anchorPosition.y
                            val dz = cameraPosition.z - anchorPosition.z
                            val distance = sqrt(dx * dx + dy * dy + dz * dz)
                            anchorNode.setModelVisibility(distance < 2f)
                        }
                    }

                }
                delay(500) // controlla ogni 0.5 secondi
            }
        }
    }

    LaunchedEffect(game.confirmedAnchorsJoiner, game.confirmedAnchorsCreator) {
        val confirmed = if (viewModel.userIsCreator()) game.confirmedAnchorsCreator
                        else game.confirmedAnchorsJoiner
        if (confirmed) {
            planeRenderer = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            view = view,
            renderer = renderer,
            cameraNode = cameraNode,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            collisionSystem = collisionSystem,
            childNodes = childNodes,
            planeRenderer = planeRenderer,
            onSessionUpdated = { session, updatedFrame ->
                frame = updatedFrame

                if (updatedFrame.camera.trackingState == TrackingState.TRACKING){
                    try {
                        mappingQuality = session.estimateFeatureMapQualityForHosting(updatedFrame.camera.pose)
                    } catch (e: NotTrackingException) {
                        Log.w("GameScreen", "Mapping quality not available: ${e.message}")
                    } catch (e: Exception) {
                        Log.e("GameScreen", "Error estimating feature map quality: ${e.message}")
                    }
                } else {
                    Log.w("GameScreen", "Camera not tracking; skipping mapping quality update.")
                }

            },
            cameraStream = rememberARCameraStream(materialLoader).apply {
                // isDepthOcclusionEnabled = true
            },
            sessionConfiguration = { session, config ->
                viewModel.updateArSession(session)
                config.depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                    Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
                // config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED
            },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { motionEvent, tappedNode ->
                    if (game.state == GameState.STARTED /* && !uiState.isHosting */) {
                        // In fase di posizionamento, se non hai già raggiunto il numero massimo di anchor
                        if (tappedNode == null && childNodes.size < game.numberOfAnchors) {
                            if (mappingQuality != Session.FeatureMapQuality.INSUFFICIENT) {
                                val hitResult = frame?.hitTest(motionEvent.x, motionEvent.y)
                                    ?.firstOrNull { it.createAnchorOrNull() != null }
                                hitResult?.createAnchorOrNull()?.let { anchor ->

                                    val anchorNode = createAnchorNode(
                                        engine = engine,
                                        modelLoader = modelLoader,
                                        materialLoader = materialLoader,
                                        model = currentModel.value,
                                        anchor = anchor
                                    )
                                    childNodes.add(anchorNode)

                                    viewModel.onAnchorPlaced(anchor, currentModel.value,
                                        onSuccess = { _ ->
                                            // leave anchor
                                        },
                                        onFailure = { error ->
                                            // Se c'è un errore durante l'hosting, rimuovi il nodo per non mostrare un oggetto "non hostato"
                                            childNodes.remove(anchorNode)
                                            Log.e("GameScreen", "Error hosting anchor: ${error.message}")
                                        }
                                    )


                                    /* viewModel.onAnchorPlaced(anchor, currentModel.value,
                                        onSuccess = { hostedAnchor ->
                                            // Solo dopo il success dell'hosting crea AnchorNode e aggiungilo al mondo AR
                                            val anchorNode = createAnchorNode(
                                                engine = engine,
                                                modelLoader = modelLoader,
                                                materialLoader = materialLoader,
                                                model = currentModel.value,
                                                anchor = hostedAnchor
                                            )
                                            childNodes.add(anchorNode)
                                        },
                                        onFailure = { error ->
                                            Log.e("GameScreen", "Error hosting anchor: ${error.message}")
                                        }
                                    ) */
                                }
                            }else{
                                // Mostra un messaggio per informare l'utente che la qualità è insufficiente
                                viewModel.onMappingQualityInsufficient()
                            }
                        }
                    } else if (game.state == GameState.HUNTING && uiState.canAddOpponentAnchors) {
                        tappedNode?.findAncestorAnchorNode()?.let { anchorNode ->
                            if (anchorNode.isVisible) {
                                val resolvedPair = uiState.resolvedOpponentAnchors.firstOrNull { it.first == anchorNode.anchor }
                                resolvedPair?.let { (_, anchorData) ->
                                    if (!tappedAnchors.contains(anchorData.cloudAnchorId)) {
                                        tappedAnchors.add(anchorData.cloudAnchorId)

                                        viewModel.onAnchorFound(anchorData.cloudAnchorId, onComplete = {
                                            childNodes.remove(anchorNode)
                                        })

                                    }
                                }
                            }
                        }
                    }
                }
            ),
            onTrackingFailureChanged = { trackingFailureReason = it }
        )

        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            val qualityText = when(mappingQuality) {
                Session.FeatureMapQuality.GOOD -> "Good Quality"
                Session.FeatureMapQuality.SUFFICIENT -> "Sufficient Quality"
                else -> "Insufficient Quality... try moving around!"
            }
            val qualityColor = when(mappingQuality) {
                Session.FeatureMapQuality.GOOD -> Color.Green.copy(alpha = 0.6f)
                Session.FeatureMapQuality.SUFFICIENT -> Color(255,165,0).copy(alpha = 0.6f) // Orange
                else -> Color.Red.copy(alpha = 0.6f)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(WindowInsets.safeContent.asPaddingValues())
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = qualityColor)
                ) {
                    Text(
                        text = qualityText,
                        fontStyle = MaterialTheme.typography.bodyLarge.fontStyle,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp, 8.dp)
                    )
                }
            }
        }


        // Overlay UI in basso
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(WindowInsets.safeContent.asPaddingValues())
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (game.state == GameState.STARTED) {
                val confirmed = if (viewModel.userIsCreator()) game.confirmedAnchorsCreator else game.confirmedAnchorsJoiner
                if (!confirmed) {
                    if (uiState.localCloudAnchors.size == game.numberOfAnchors && !uiState.isHosting) {
                        //placed anchors are all placed but not confirmed -> show confirm button
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Button(
                                onClick = { viewModel.confirmAnchors() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Confirm Anchors", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }else{
                        if (uiState.isHosting){
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        }
                        if (game.numberOfAnchors - childNodes.size > 0) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(
                                    text = "Place ${game.numberOfAnchors - childNodes.size} objects on a plane",
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "Waiting for opponent to confirm anchors...",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }else if (game.state == GameState.HUNTING) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.resolvedOpponentAnchors.size < game.numberOfAnchors) {
                        if (uiState.showRetryButton) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Button(
                                    onClick = { viewModel.retryResolveOpponentAnchors() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Retry Anchor Resolution", color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = "Resolving ${game.numberOfAnchors - uiState.resolvedOpponentAnchors.size} opponent anchors...",
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        textAlign = TextAlign.Center
                                    )
                                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                                }
                            }
                        }
                    } else if (uiState.canAddOpponentAnchors) {
                        // Indicatore findings/total
                        val totalAnchors = game.numberOfAnchors

                        val (user, opponent) = if (viewModel.userIsCreator()) {
                            game.creator to game.joiner
                        } else {
                            game.joiner to game.creator
                        }

                        val userFoundedAnchors = user?.anchorsFound ?: 0
                        val opponentFoundedAnchors = opponent?.anchorsFound ?: 0
                        val userDisplayName = user?.displayName.takeUnless { it.isNullOrBlank() } ?: "You"
                        val opponentDisplayName = opponent?.displayName.takeUnless { it.isNullOrBlank() } ?: "Opponent"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card( // Opponent findings counter
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(
                                    text = "$opponentDisplayName: $opponentFoundedAnchors / $totalAnchors",
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier
                                        .padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Card( // User findings counter
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(3.dp, MaterialTheme.colorScheme.onPrimary),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = "$userDisplayName: $userFoundedAnchors / $totalAnchors",
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(
                                text = "Find the opponent's anchors!",
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "Waiting for opponent to resolve all anchors...",
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (game.state == GameState.ENDED) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Button(
                            onClick = { viewModel.exitGame(popUpScreen) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Exit", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    val color = if (game.winner == viewModel.getUserProfile().id){
                        Color.Green
                    } else Color.Red
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = color)
                    ) {
                        Text(
                            text = when (game.winner) {
                                null -> "Someone disconnected!"
                                viewModel.getUserProfile().id -> "You won!"
                                else -> "You lost!"
                            },
                            fontSize = 32.sp,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

        }
        /* if (uiState.isHosting) { // hosting anchor (waiting with circular progress)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Text("Hosting anchor...", fontSize = 18.sp, color = Color.White)
                }
            }
        } */

    }

    BackHandler { viewModel.onBackPressed() }
    if (viewModel.showExitConfirmation) {
        ExitDialog(
            title = "Exit Game?",
            text = "Do you really want to exit? The game will be ended.",
            confirmText = "Exit",
            dismissText = "Cancel",
            onDismissRequest = { viewModel.dismissExitConfirmation() },
            onConfirm = {
                viewModel.endGameAndExit(popUpScreen)
                viewModel.dismissExitConfirmation()
            }
        )
    }
}


fun createAnchorNode(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    model: String,
    anchor: Anchor,
    visible: Boolean = true
): AnchorNode {
    val anchorNode = AnchorNode(engine = engine, anchor = anchor).apply {
        isPositionEditable = false // true by default
        isRotationEditable = false
        // isScaleEditable = true
        // isSmoothTransformEnabled = true
        // isEditable = true
        isVisible = visible
    }

    // Create 3D model node to attach to the anchor
    val modelInstance = modelLoader.createInstance(
        modelLoader.createModel(assetFileLocation = "models/${model}.glb")
    )!!

    val modelNode = ModelNode(modelInstance, scaleToUnits = 0.25f)
        .apply { // child of anchorNode
            rotation = Rotation(0f,-90f,0f) //for chest
            isPositionEditable = false // true by default
            isRotationEditable = false
            // isEditable = true
            // isScaleEditable = true
            // isSmoothTransformEnabled = true
            isVisible = visible

    }

    val boundingBoxNode = CubeNode(
        engine,
        size = modelNode.extents,
        center = modelNode.center,
        materialInstance = materialLoader.createColorInstance(Color.White.copy(alpha = 0.5f))
    ).apply { isVisible = false }
    modelNode.addChildNode(boundingBoxNode)

    anchorNode.addChildNode(modelNode)

    listOf(modelNode, anchorNode).forEach { node -> // "listOf" used to not repeat the same code twice
        node.onEditingChanged = { editingTransforms ->
            boundingBoxNode.isVisible =
                editingTransforms.isNotEmpty() // check if the model is being edited
        }
    }
    return anchorNode
}

fun Node.findAncestorAnchorNode(): AnchorNode? {
    var current: Node? = this
    while (current != null) {
        if (current is AnchorNode) return current
        current = current.parent
    }
    return null
}

fun AnchorNode.setModelVisibility(visible: Boolean) {
    this.isVisible = visible
    childNodes.forEach { child ->
        if (child is ModelNode) {
            child.isVisible = visible
        }
    }
}

fun Pose.toVector3(): Vector3 {
    return Vector3(tx(), ty(), tz())
}


@Preview(showSystemUi = true)
@Composable
fun MappingQualityPreview() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(WindowInsets.safeContent.asPaddingValues())
            .padding(16.dp)
            .fillMaxWidth(),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.6f))
        ) {
            Text(
                text = "Insufficient Quality... try moving around!",
                fontStyle = MaterialTheme.typography.bodyLarge.fontStyle,
                // fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.padding(16.dp, 8.dp)
            )
        }
    }
}