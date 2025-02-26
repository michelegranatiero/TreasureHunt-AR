package com.example.treasurehunt_ar.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.treasurehunt_ar.MatchmakingMode
import com.example.treasurehunt_ar.Route
import com.example.treasurehunt_ar.TreasureHuntApplication
import com.example.treasurehunt_ar.model.GameState
import com.example.treasurehunt_ar.ui.utils.QRCodeDisplay
import com.example.treasurehunt_ar.ui.utils.QRScannerButton
import com.example.treasurehunt_ar.ui.utils.customViewModelFactory

@Composable
fun MatchmakingScreen(
    restartApp: (Route) -> Unit,
    openAndPopUp: (Route, Route) -> Unit,
    popUpScreen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MatchmakingViewModel = viewModel<MatchmakingViewModel>(
        factory = customViewModelFactory { savedStateHandle ->
            MatchmakingViewModel(
                TreasureHuntApplication.serviceModule,
                savedStateHandle
            )
        }
    ),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.initialize(restartApp, openAndPopUp) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .imePadding()
            .windowInsetsPadding(WindowInsets.safeContent)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (uiState.matchmakingMode) {
            MatchmakingMode.CREATE -> MatchmakingCreateScreen(uiState, viewModel, popUpScreen)
            MatchmakingMode.JOIN -> MatchmakingJoinScreen(uiState, viewModel, popUpScreen)
        }
    }
}


@Composable
fun MatchmakingCreateScreen(
    uiState: MatchmakingUiState,
    viewModel: MatchmakingViewModel,
    popUpScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current


    if (uiState.game?.state != GameState.STARTED) {
        Text(text = "Select number of anchors to find:")

        val options = (1..5).toList()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEach { value ->
                ElevatedFilterChip(
                    selected = uiState.numberOfAnchors == value,
                    onClick = { viewModel.updateNumAnchors(value) },
                    label = { Text(
                        value.toString(),
                        fontSize = 24.sp,
                        modifier = Modifier.padding(2.dp, 8.dp)
                    )
                    }
                )
            }
        }
        Spacer(modifier = Modifier.padding(16.dp))
        Text("Game Code:", fontSize = 24.sp)
        TextButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(uiState.roomCode))
            },
            modifier = Modifier.padding(8.dp)

        ) { Text(uiState.roomCode)}

        val qrCodeSizeDp = 200f
        Column(
            modifier = Modifier
                .background(Color.White)
                .size(qrCodeSizeDp.dp, qrCodeSizeDp.dp)
        ) {
            if (uiState.roomCode != "") {
                QRCodeDisplay(uiState.roomCode, sizeInDp = qrCodeSizeDp)
            }
        }
        Spacer(modifier = Modifier.padding(16.dp))
    }





    when (uiState.game?.state) {
        GameState.JOINED -> {
            Text(
                "${uiState.game.joiner?.displayName?.takeIf { it.isNotEmpty() } ?: "A new User"} has joined the game.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            Button(onClick = { viewModel.startGame() }) {
                Text("Start Game")
            }
        }
        GameState.STARTED -> {
            Text(
                "Starting Game...",
            )
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }
        else -> {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            Text(
                "Waiting for a player to join...",
                modifier = Modifier.padding(8.dp)
            )
            Spacer(modifier = Modifier.padding(8.dp))

        }
    }

    Button(
        onClick = {
            focusManager.clearFocus()
            viewModel.exitGame { popUpScreen() }
        },
        modifier = Modifier
            .padding(16.dp)
    ) {
        Text("Exit Game")
    }
    BackHandler { viewModel.exitGame { popUpScreen() } }
}

@Composable
fun MatchmakingJoinScreen(
    uiState: MatchmakingUiState,
    viewModel: MatchmakingViewModel,
    popUpScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    if (uiState.game?.state == GameState.STARTED) {
        Text(
            "Starting Game...",
            fontSize = 24.sp
        )
        CircularProgressIndicator()
    }
    //GAME JOINED
    if (uiState.game?.state == GameState.JOINED){
        Text("Game Joined.",
            fontSize = 24.sp
        )
        Text(
            "Waiting for ${uiState.game.creator?.displayName?.takeIf { it.isNotEmpty() } ?: "the opponent"} to start the game...",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.exitGameAndResetViewModel()
            },
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text("Exit Game")
        }
        BackHandler { viewModel.exitGameAndResetViewModel() }
    } else if (uiState.game == null) { // GAME NOT JOINED YET
        if (uiState.joiningRoom) {
            Text("Joining Game...")
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }else{
            Text("Join a Game:")
            QRScannerButton ("Scan QR code", onScanComplete = { code ->
                viewModel.handleQRScanResult(code)
            },
                modifier = Modifier.padding(16.dp)
            )
            OutlinedTextField(
                value = uiState.roomCode,
                onValueChange = { viewModel.updateRoomCode(it) },
                label = { Text("Game Code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                )
            )
            Button(onClick = {
                focusManager.clearFocus()
                viewModel.joinRoom()
            }, modifier = Modifier.padding(16.dp)
            ) {
                Text("Join Game")
            }
            Button(onClick = {
                focusManager.clearFocus()
                viewModel.exitMatchmaking(popUpScreen)
            }) {
                Text("Exit")
            }
        }
        BackHandler { viewModel.exitMatchmaking(popUpScreen) }
    }
}

