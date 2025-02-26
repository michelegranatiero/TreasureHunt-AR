package com.example.treasurehunt_ar

import android.annotation.SuppressLint
import android.os.Build
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
import com.example.treasurehunt_ar.ui.game.GameScreen
import com.example.treasurehunt_ar.ui.game.HomeScreen
import com.example.treasurehunt_ar.ui.game.MatchmakingScreen
import com.example.treasurehunt_ar.ui.splash.SplashScreen
import com.example.treasurehunt_ar.ui.theme.TreasureHunt_ARTheme
import com.example.treasurehunt_ar.ui.utils.SnackbarFlowHelper
import com.example.treasurehunt_ar.ui.utils.checkGooglePlayServicesForAR

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // To use this use usesCleartextTraffic in Manifest
        // configureFirebaseServices() //Only for debugging

        setContent {
            //for navigation bottom bar (if needed)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            TreasureHunt_ARTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val snackbarHostState = remember { SnackbarHostState() }
                    val appNavState = rememberAppState(/* snackbarHostState */)
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
                            navController = appNavState.navController,
                            startDestination = Route.Splash,
                            // modifier = Modifier.padding(innerPaddingModifier)
                        ) {
                            composable<Route.Splash> {
                                SplashScreen(
                                    openAndPopUp = { route, popUp -> appNavState.navigateAndPopUp(route, popUp) }
                                )
                            }
                            composable<Route.AccountCenter> {
                                AccountCenterScreen(
                                    restartApp = { route -> appNavState.clearAndNavigate(route) },
                                    openScreen = { route -> appNavState.navigate(route) }
                                )
                            }
                            authenticationGraph(appNavState)
                            gameGraph(appNavState)
                        }
                    }

                }


            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkGooglePlayServicesForAR(activity = this)
    }
}




fun NavGraphBuilder.authenticationGraph(appNavState: AppNavState) {
    navigation<Route.AuthenticationGraph>(
        startDestination = Route.AuthenticationGraph.Login
    ){
        composable<Route.AuthenticationGraph.Login> {
            LoginScreen(
                openAndPopUp = { route, popUp -> appNavState.navigateAndPopUp(route, popUp) }
            )
        }
        composable<Route.AuthenticationGraph.Registration> {
            RegistrationScreen(
                openAndPopUp = { route, popUp -> appNavState.navigateAndPopUp(route, popUp) }
            )
        }
    }
}

fun NavGraphBuilder.gameGraph(appNavState: AppNavState) {
    navigation<Route.GameGraph>(
        startDestination = Route.GameGraph.Home
    ){
        composable<Route.GameGraph.Home> {
            HomeScreen(
                restartApp = { route -> appNavState.clearAndNavigate(route) },
                openScreen = { route -> appNavState.navigate(route) }
            )
        }
        composable<Route.GameGraph.Matchmaking> { /* entry -> */
            // val matchmaking = entry.toRoute<Route.GameGraph.Matchmaking>()
            MatchmakingScreen(
                // matchmaking.mode,
                restartApp = { route -> appNavState.clearAndNavigate(route) },
                openAndPopUp = { route, popUp -> appNavState.navigateAndPopUp(route, popUp) },
                popUpScreen = { appNavState.popUp() }
            )
        }
        composable<Route.GameGraph.Game> {
            GameScreen(
                restartApp = { route -> appNavState.clearAndNavigate(route) },
                popUpScreen = { appNavState.popUp() }
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
): AppNavState {
    return remember(navController/* , snackbarHostState, snackbarManager, coroutineScope */) {
        AppNavState(navController/* , snackbarHostState, snackbarManager, coroutineScope */)
    }
}
