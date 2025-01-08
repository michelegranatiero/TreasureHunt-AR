package com.example.ar_firstapp.navigation

import kotlinx.serialization.Serializable

/*
sealed interface Route {
    //Login Graph
    @Serializable
    data object LoginGraph: Route

    @Serializable
    data object Login: Route

    @Serializable
    data object Registration: Route

    //Game Graph
    @Serializable
    data object GameGraph: Route

    @Serializable
    data object Home: Route
}*/


sealed interface Route {

    // Login Graph
    @Serializable
    data object LoginGraph : Route {
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
