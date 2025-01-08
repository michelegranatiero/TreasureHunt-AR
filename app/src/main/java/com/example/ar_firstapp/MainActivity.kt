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
import com.example.ar_firstapp.navigation.Route
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
                    startDestination = Route.LoginGraph
                ) {
                    loginGraph()
                    gameGraph()
                }

            }
        }
    }
}




fun NavGraphBuilder.loginGraph() {
    navigation<Route.LoginGraph>(
        startDestination = Route.LoginGraph.Login
    ){
        composable<Route.LoginGraph.Login> {
            LoginScreen()
        }
        composable<Route.LoginGraph.Registration> {
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