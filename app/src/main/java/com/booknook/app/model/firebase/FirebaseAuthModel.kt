package com.booknook.app.model.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthModel(
    private val auth: FirebaseAuth
) {
    private val authUserId = MutableStateFlow(auth.currentUser?.uid)

    init {
        auth.addAuthStateListener { firebaseAuth ->
            authUserId.value = firebaseAuth.currentUser?.uid
        }
    }

    fun currentUserId(): String? = auth.currentUser?.uid

    fun requireUserId(): String = currentUserId() ?: throw IllegalStateException("Not logged in")

    fun observeAuthUserId(): StateFlow<String?> = authUserId

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(email: String, password: String, username: String): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw IllegalStateException("Registration failed: No User")
        user.updateProfile(userProfileChangeRequest { displayName = username }).await()
        return user.uid
    }

    fun logout() {
        auth.signOut()
    }
}
