package com.example.ar_firstapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.ar_firstapp.ui.game.HomeScreen
import com.example.ar_firstapp.ui.authentication.LoginScreen
import com.example.ar_firstapp.ui.authentication.RegistrationScreen
import com.example.ar_firstapp.ui.theme.AR_FirstAppTheme
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.auth.auth


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        configureFirebaseServices() //Only for debugging

        setContent {
            AR_FirstAppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val appState = rememberAppState()

                    Scaffold { innerPaddingModifier ->
                        NavHost(
                            navController = appState.navController,
                            startDestination = Route.AuthenticationGraph,
                            modifier = Modifier.padding(innerPaddingModifier)
                        ) {
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


fun NavGraphBuilder.authenticationGraph(appState: AppState) {
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

fun NavGraphBuilder.gameGraph(appState: AppState) {
    navigation<Route.GameGraph>(
        startDestination = Route.GameGraph.Home
    ){
        composable<Route.GameGraph.Home> {
            HomeScreen()
        }
    }
}



@Composable
fun rememberAppState(navController: NavHostController = rememberNavController()): AppState {
    return remember(navController) {
        AppState(navController)
    }
}

@Stable
class AppState(val navController: NavHostController) {
    fun popUp() {
        navController.popBackStack()
    }

    fun navigate(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }

    fun navigateAndPopUp(route: Route, popUp: Route) {
        navController.navigate(route) {
            launchSingleTop = true
            popUpTo(popUp) { inclusive = true }
        }
    }

    fun clearAndNavigate(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            popUpTo(0) { inclusive = true }
        }
    }
}