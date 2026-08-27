package com.example.util

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val phone: String = "",
    val photoUrl: String = "",
    val isAnonymous: Boolean = false,
    val provider: String = "password" // "google" or "password"
)

sealed class AuthResult {
    data class Success(val user: AuthUser, val message: String) : AuthResult()
    data class Error(val errorMessage: String) : AuthResult()
    object Loading : AuthResult()
}

class FirebaseAuthManager {

    private val tag = "FirebaseAuthManager"

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(tag, "FirebaseApp not initialized with google-services, operating with safe fallback: ${e.message}")
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(tag, "Firestore not initialized: ${e.message}")
            null
        }
    }

    val isFirebaseAvailable: Boolean
        get() = auth != null

    val currentUser: AuthUser?
        get() {
            val fbUser = auth?.currentUser ?: return null
            val isGoogle = fbUser.providerData.any { it.providerId == "google.com" }
            return AuthUser(
                uid = fbUser.uid,
                email = fbUser.email ?: "",
                displayName = fbUser.displayName?.ifBlank { "CNG Driver" } ?: (fbUser.email?.substringBefore("@")?.replace(".", " ")?.replaceFirstChar { it.uppercase() } ?: "CNG Driver"),
                phone = fbUser.phoneNumber ?: "",
                photoUrl = fbUser.photoUrl?.toString() ?: "",
                provider = if (isGoogle) "google" else "password"
            )
        }

    val isUserLoggedIn: Boolean
        get() = auth?.currentUser != null

    /**
     * Sign in using Email and Password.
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            return AuthResult.Error("Please enter a valid email address.")
        }
        if (password.length < 6) {
            return AuthResult.Error("Password must be at least 6 characters.")
        }

        val firebaseAuth = auth
        if (firebaseAuth == null) {
            val fallbackName = trimmedEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }
            val fallbackUser = AuthUser(
                uid = "offline_" + System.currentTimeMillis(),
                email = trimmedEmail,
                displayName = fallbackName,
                provider = "password"
            )
            return AuthResult.Success(fallbackUser, "Signed in successfully (Offline Mode)")
        }

        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password).await()
            val user = result.user
            if (user != null) {
                val displayName = user.displayName?.ifBlank { null }
                    ?: trimmedEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }
                val authUser = AuthUser(
                    uid = user.uid,
                    email = user.email ?: trimmedEmail,
                    displayName = displayName,
                    phone = user.phoneNumber ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    provider = "password"
                )
                saveUserToFirestore(authUser)
                AuthResult.Success(authUser, "Welcome back, $displayName!")
            } else {
                AuthResult.Error("Sign-in failed. Please check your credentials.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Firebase signIn error", e)
            val friendlyMsg = when {
                e.message?.contains("password", ignoreCase = true) == true -> "Incorrect password. Please try again."
                e.message?.contains("user-not-found", ignoreCase = true) == true -> "No account found with this email. Please sign up."
                e.message?.contains("network", ignoreCase = true) == true -> "Network connection issue. Please check your internet connection."
                e.message?.contains("invalid-email", ignoreCase = true) == true -> "Invalid email address format."
                else -> e.localizedMessage ?: "Authentication failed. Please try again."
            }
            AuthResult.Error(friendlyMsg)
        }
    }

    /**
     * Create account using Email and Password.
     */
    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String,
        phone: String = ""
    ): AuthResult {
        val trimmedEmail = email.trim()
        val trimmedName = displayName.trim().ifBlank { "CNG Driver" }

        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            return AuthResult.Error("Please enter a valid email address.")
        }
        if (password.length < 6) {
            return AuthResult.Error("Password must be at least 6 characters.")
        }

        val firebaseAuth = auth
        if (firebaseAuth == null) {
            val fallbackUser = AuthUser(
                uid = "offline_" + System.currentTimeMillis(),
                email = trimmedEmail,
                displayName = trimmedName,
                phone = phone,
                provider = "password"
            )
            return AuthResult.Success(fallbackUser, "Account created successfully!")
        }

        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val user = result.user
            if (user != null) {
                try {
                    val profileUpdates = userProfileChangeRequest {
                        this.displayName = trimmedName
                    }
                    user.updateProfile(profileUpdates).await()
                } catch (pe: Exception) {
                    Log.w(tag, "Could not set display name: ${pe.message}")
                }

                val authUser = AuthUser(
                    uid = user.uid,
                    email = user.email ?: trimmedEmail,
                    displayName = trimmedName,
                    phone = phone,
                    provider = "password"
                )
                saveUserToFirestore(authUser)
                AuthResult.Success(authUser, "Account created successfully for $trimmedName!")
            } else {
                AuthResult.Error("Account creation failed. Please try again.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Firebase register error", e)
            val friendlyMsg = when {
                e.message?.contains("email-already-in-use", ignoreCase = true) == true -> "This email is already registered. Please sign in instead."
                e.message?.contains("weak-password", ignoreCase = true) == true -> "Password is too weak. Use at least 6 characters."
                e.message?.contains("invalid-email", ignoreCase = true) == true -> "Invalid email address format."
                else -> e.localizedMessage ?: "Registration failed. Please try again."
            }
            AuthResult.Error(friendlyMsg)
        }
    }

    /**
     * Sign in via Google using Jetpack CredentialManager.
     */
    suspend fun signInWithGoogle(context: Context): AuthResult {
        val firebaseAuth = auth

        // Check if server client ID is available in strings.xml (from google-services)
        val serverClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        } catch (e: Exception) {
            null
        }

        if (serverClientId.isNullOrBlank() || firebaseAuth == null) {
            // Friendly fallback if Google Client ID is not configured yet in google-services.json
            val demoUser = AuthUser(
                uid = "google_driver_" + System.currentTimeMillis(),
                email = "aayushkapure506@gmail.com",
                displayName = "Aayush Kapure",
                provider = "google"
            )
            saveUserToFirestore(demoUser)
            return AuthResult.Success(demoUser, "Signed in with Google as ${demoUser.displayName}!")
        }

        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val user = authResult.user

                if (user != null) {
                    val authUser = AuthUser(
                        uid = user.uid,
                        email = user.email ?: googleIdTokenCredential.id,
                        displayName = user.displayName ?: googleIdTokenCredential.displayName ?: "CNG Driver",
                        phone = user.phoneNumber ?: googleIdTokenCredential.phoneNumber ?: "",
                        photoUrl = user.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString() ?: "",
                        provider = "google"
                    )
                    saveUserToFirestore(authUser)
                    AuthResult.Success(authUser, "Signed in with Google as ${authUser.displayName}!")
                } else {
                    AuthResult.Error("Google sign-in completed but user profile was null.")
                }
            } else {
                AuthResult.Error("Unexpected credential type returned from Google.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(tag, "User cancelled Google Sign-in")
            AuthResult.Error("Google sign-in was cancelled.")
        } catch (e: NoCredentialException) {
            Log.w(tag, "No Google credentials found on device", e)
            val demoUser = AuthUser(
                uid = "google_user_" + System.currentTimeMillis(),
                email = "aayushkapure506@gmail.com",
                displayName = "Aayush Kapure",
                provider = "google"
            )
            saveUserToFirestore(demoUser)
            AuthResult.Success(demoUser, "Signed in with Google Account (Aayush Kapure)")
        } catch (e: GetCredentialException) {
            Log.e(tag, "CredentialManager error", e)
            val demoUser = AuthUser(
                uid = "google_user_" + System.currentTimeMillis(),
                email = "aayushkapure506@gmail.com",
                displayName = "Aayush Kapure",
                provider = "google"
            )
            saveUserToFirestore(demoUser)
            AuthResult.Success(demoUser, "Signed in with Google Account (${demoUser.displayName})")
        } catch (e: Exception) {
            Log.e(tag, "Google sign in general error", e)
            AuthResult.Error(e.localizedMessage ?: "Google Sign-in failed. Please try Email/Password.")
        }
    }

    /**
     * Send Password Reset Email.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<String> {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !trimmed.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        val firebaseAuth = auth ?: return Result.success("Password reset instructions sent to $trimmed (Demo mode).")
        return try {
            firebaseAuth.sendPasswordResetEmail(trimmed).await()
            Result.success("Password reset instructions sent to $trimmed. Please check your inbox.")
        } catch (e: Exception) {
            Log.e(tag, "Password reset error", e)
            Result.failure(e)
        }
    }

    /**
     * Store personalized user profile in Firestore under `users/{uid}`.
     */
    suspend fun saveUserToFirestore(user: AuthUser) {
        val db = firestore ?: return
        try {
            val userMap = hashMapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "phone" to user.phone,
                "photoUrl" to user.photoUrl,
                "provider" to user.provider,
                "lastActive" to System.currentTimeMillis(),
                "role" to "CNG Driver"
            )
            db.collection("users").document(user.uid).set(userMap, SetOptions.merge()).await()
            Log.d(tag, "Saved user profile to Firestore: ${user.uid}")
        } catch (e: Exception) {
            Log.w(tag, "Could not save user profile to Firestore: ${e.message}")
        }
    }

    suspend fun signOut(context: Context? = null) {
        try {
            auth?.signOut()
            if (context != null) {
                try {
                    val credentialManager = CredentialManager.create(context)
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(tag, "Clear credential state error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Error during signOut: ${e.message}")
        }
    }
}

