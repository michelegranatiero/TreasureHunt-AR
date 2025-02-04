package com.example.treasurehunt_ar

import kotlinx.serialization.Serializable

sealed interface Route {

    // Splash
    @Serializable
    data object Splash : Route

    @Serializable
    data object AccountCenter : Route

    // Login Graph
    @Serializable
    data object AuthenticationGraph : Route {
        @Serializable
        data object Login : Route
        @Serializable
        data object Registration : Route
    }

    // Game Graph
    @Serializable
    data object GameGraph : Route {
        @Serializable
        data object Home : Route
    }
}

const val LOCALHOST = "192.168.1.56" //"10.0.2.2"
const val AUTH_PORT = 9099
const val FIRESTORE_PORT = 8080