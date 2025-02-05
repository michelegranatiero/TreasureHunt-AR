package com.example.treasurehunt_ar

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.safeContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.treasurehunt_ar.ui.account_center.AccountCenterScreen
import com.example.treasurehunt_ar.ui.authentication.LoginScreen
import com.example.treasurehunt_ar.ui.authentication.RegistrationScreen
import com.example.treasurehunt_ar.ui.game.HomeScreen
import com.example.treasurehunt_ar.ui.splash.SplashScreen
import com.example.treasurehunt_ar.ui.theme.TreasureHunt_ARTheme
import com.example.treasurehunt_ar.ui.utils.SnackbarFlowHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // To use this use usesCleartextTraffic in Manifest
        configureFirebaseServices() //Only for debugging

        setContent {
            //for navigation bottom bar (if needed)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            TreasureHunt_ARTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val snackbarHostState = remember { SnackbarHostState() }
                    val appState = rememberAppState(/* snackbarHostState */)
                    SnackbarFlowHelper(snackbarHostState)
                    Scaffold (
                        snackbarHost = { SnackbarHost(
                            hostState = snackbarHostState,
                            snackbar = { data -> Snackbar(
                                snackbarData = data,
                                // containerColor = MaterialTheme.colorScheme.primaryContainer,
                                // contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) }
                        ) },

                    ){ /* innerPaddingModifier -> */
                        NavHost(
                            navController = appState.navController,
                            startDestination = Route.Splash,
                            // modifier = Modifier.padding(innerPaddingModifier)
                        ) {
                            composable<Route.Splash> {
                                SplashScreen(
                                    openAndPopUp = { route, popUp -> appState.navigateAndPopUp(route, popUp) }
                                )
                            }
                            composable<Route.AccountCenter> {
                                AccountCenterScreen(
                                    restartApp = { route -> appState.clearAndNavigate(route) },
                                    openScreen = { route -> appState.navigate(route) }
                                )
                            }
                            authenticationGraph(appState)
                            gameGraph(appState)
                        }
                    }

                }


            }
        }
    }

    private fun configureFirebaseServices() {
        Firebase.auth.useEmulator(LOCALHOST, AUTH_PORT)
        // Firebase.firestore.useEmulator(LOCALHOST, FIRESTORE_PORT)
    }
}



fun NavGraphBuilder.authenticationGraph(appState: MainAppState) {
    navigation<Route.AuthenticationGraph>(
        startDestination = Route.AuthenticationGraph.Login
    ){
        composable<Route.AuthenticationGraph.Login> {
            LoginScreen(
                openAndPopUp = { route, popUp -> appState.navigateAndPopUp(route, popUp) }
            )
        }
        composable<Route.AuthenticationGraph.Registration> {
            RegistrationScreen(
                openAndPopUp = { route, popUp -> appState.navigateAndPopUp(route, popUp) }
            )
        }
    }
}

fun NavGraphBuilder.gameGraph(appState: MainAppState) {
    navigation<Route.GameGraph>(
        startDestination = Route.GameGraph.Home
    ){
        composable<Route.GameGraph.Home> {
            HomeScreen(
                restartApp = { route -> appState.clearAndNavigate(route) },
                openScreen = { route -> appState.navigate(route) }
            )
        }
    }
}



@Composable
fun rememberAppState(
    navController: NavHostController = rememberNavController(),
    /* snackbarHostState: SnackbarHostState,
    snackbarManager: SnackbarManager = SnackbarManager,
    coroutineScope: CoroutineScope = rememberCoroutineScope() */
): MainAppState {
    return remember(navController/* , snackbarHostState, snackbarManager, coroutineScope */) {
        MainAppState(navController/* , snackbarHostState, snackbarManager, coroutineScope */)
    }
}
