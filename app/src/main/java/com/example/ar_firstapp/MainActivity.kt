package com.example.ar_firstapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.ar_firstapp.ui.game.HomeScreen
import com.example.ar_firstapp.ui.authentication.LoginScreen
import com.example.ar_firstapp.ui.authentication.RegistrationScreen
import com.example.ar_firstapp.ui.theme.AR_FirstAppTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AR_FirstAppTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Route.AuthenticationGraph
                ) {
                    authenticationGraph()
                    gameGraph()
                }

            }
        }
    }
}




fun NavGraphBuilder.authenticationGraph() {
    navigation<Route.AuthenticationGraph>(
        startDestination = Route.AuthenticationGraph.Login
    ){
        composable<Route.AuthenticationGraph.Login> {
            LoginScreen()
        }
        composable<Route.AuthenticationGraph.Registration> {
            RegistrationScreen()
        }
    }
}

fun NavGraphBuilder.gameGraph() {
    navigation<Route.GameGraph>(
        startDestination = Route.GameGraph.Home
    ){
        composable<Route.GameGraph.Home> {
            HomeScreen()
        }
    }
}