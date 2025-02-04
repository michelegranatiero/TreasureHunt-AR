package com.example.treasurehunt_ar

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

        configureFirebaseServices() //Only for debugging

        setContent {
            TreasureHunt_ARTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val snackbarHostState = remember { SnackbarHostState() }
                    val appState = rememberAppState(/* snackbarHostState */)
                    SnackbarFlowHelper(snackbarHostState)
                    Scaffold (
                        // contentWindowInsets = WindowInsets(0.dp),
                        snackbarHost = { SnackbarHost(
                            hostState = snackbarHostState,
                            snackbar = { data -> Snackbar(
                                snackbarData = data,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ) }
                        ) }
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
