package com.yanivrw.lessscreen.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.yanivrw.lessscreen.GoogleConfig
import com.yanivrw.lessscreen.data.models.Profile
import com.yanivrw.lessscreen.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.util.UUID

object AuthRepository {

    val sessionStatus: Flow<SessionStatus> = supabase.auth.sessionStatus

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUp(email: String, password: String) {
        if (supabase.auth.currentUserOrNull() != null) supabase.auth.signOut()
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signInWithGoogle(context: Context) {
        // Generate a nonce — required by Google to prevent replay attacks.
        // We send the SHA-256 hash to Google and the raw value to Supabase;
        // Supabase hashes it again and checks it matches what Google received.
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = MessageDigest.getInstance("SHA-256")
            .digest(rawNonce.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val credentialManager = CredentialManager.create(context)

        val credential = try {
            // Primary: full account picker (new users)
            credentialManager.getCredential(
                context,
                GetCredentialRequest.Builder()
                    .addCredentialOption(
                        GetSignInWithGoogleOption.Builder(GoogleConfig.WEB_CLIENT_ID)
                            .setNonce(hashedNonce)
                            .build()
                    )
                    .build()
            ).credential
        } catch (_: Exception) {
            // Fallback: returning-user flow
            credentialManager.getCredential(
                context,
                GetCredentialRequest.Builder()
                    .addCredentialOption(
                        GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(GoogleConfig.WEB_CLIENT_ID)
                            .setNonce(hashedNonce)
                            .build()
                    )
                    .build()
            ).credential
        }

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            supabase.auth.signInWith(IDToken) {
                idToken = googleIdTokenCredential.idToken
                provider = Google
                nonce = rawNonce   // raw nonce — Supabase hashes it to verify against Google
            }
        } else {
            error("Unexpected credential type: ${credential.type}")
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun myProfile(): Profile? {
        val id = currentUserId() ?: return null
        return supabase.from("profiles")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<Profile>()
    }

    suspend fun setDisplayName(name: String) {
        val id = currentUserId() ?: return
        supabase.from("profiles").update(mapOf("display_name" to name)) {
            filter { eq("id", id) }
        }
    }
}
