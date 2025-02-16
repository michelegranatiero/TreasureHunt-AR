package com.example.treasurehunt_ar.model.service.impl

import com.example.treasurehunt_ar.model.User
import com.example.treasurehunt_ar.model.service.AccountService
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AccountServiceImpl: AccountService {

    private val auth = Firebase.auth

    override val currentUser: Flow<User?>
        get() = callbackFlow {
            val listener =
                FirebaseAuth.AuthStateListener { auth ->
                    this.trySend(auth.currentUser.toAppUser())
                }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }

    override val currentUserId: String
        get() = auth.currentUser?.uid.orEmpty()

    override fun hasUser(): Boolean {
        return auth.currentUser != null
    }

    override fun getUserProfile(): User {
        return auth.currentUser.toAppUser()
    }

    override suspend fun updateDisplayName(newDisplayName: String) {
        val profileUpdates = userProfileChangeRequest {
            displayName = newDisplayName
        }

        auth.currentUser!!.updateProfile(profileUpdates).await()
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun signUpWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
    }
    // OR (IF USER HAS ANONYMOUS ACCOUNT AT STARTUP)
    override suspend fun linkAccountWithEmail(email: String, password: String) {
        val credential = EmailAuthProvider.getCredential(email, password)
        auth.currentUser!!.linkWithCredential(credential).await()
    }

    override suspend fun signInWithGoogle(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(firebaseCredential).await()
    }

    // ONLY WORKS IF USER HAS ANONYMOUS ACCOUNT AT STARTUP
    override suspend fun linkAccountWithGoogle(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        // note that auth.currentUser is null if user does not have anonymous account
        try {
            // Prova a collegare l'account anonimo a quello Google
            auth.currentUser!!.linkWithCredential(firebaseCredential).await()
        } catch (e: FirebaseAuthUserCollisionException) {
            // Se l'account Google è già linkato, effettua il sign-in
            auth.signInWithCredential(firebaseCredential).await()
        }
    }

    override suspend fun createAnonymousAccount() {
        auth.signInAnonymously().await()
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun deleteAccount() {
        auth.currentUser!!.delete().await()
    }

    private fun FirebaseUser?.toAppUser(): User {
        return if (this == null) User() else User(
            id = this.uid,
            email = this.email ?: "",
            provider = this.providerId,
            displayName = this.displayName ?: "",
            isAnonymous = this.isAnonymous
        )
    }
}