package com.yanivrw.lessscreen.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
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

object AuthRepository {

    val sessionStatus: Flow<SessionStatus> = supabase.auth.sessionStatus

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUp(email: String, password: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signInWithGoogle(context: Context) {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)   // show all accounts, not just pre-authorized
            .setServerClientId(GoogleConfig.WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            supabase.auth.signInWith(IDToken) {
                idToken = googleIdTokenCredential.idToken
                provider = Google
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
