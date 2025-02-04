package com.example.treasurehunt_ar.model.service

import com.example.treasurehunt_ar.model.User
import kotlinx.coroutines.flow.Flow

interface AccountService {
    val currentUser: Flow<User?>
    val currentUserId: String
    fun hasUser(): Boolean
    fun getUserProfile(): User
    suspend fun createAnonymousAccount()
    suspend fun updateDisplayName(newDisplayName: String)
    suspend fun signInWithEmail(email: String, password: String)
    suspend fun signUpWithEmail(email: String, password: String) // not needed if create anonymous account at startup (use linkAccountWithEmail)
    suspend fun linkAccountWithEmail(email: String, password: String) // if user has anonymous account
    suspend fun signInWithGoogle(idToken: String)
    suspend fun linkAccountWithGoogle(idToken: String) // if user has anonymous account
    suspend fun signOut()
    suspend fun deleteAccount()
}