package com.yanivrw.lessscreen.data

import com.yanivrw.lessscreen.supabase
import com.yanivrw.lessscreen.data.models.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
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
        // If email confirmation is disabled, the user is signed in immediately.
        // If enabled, the user has to confirm via email link before signing in.
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
