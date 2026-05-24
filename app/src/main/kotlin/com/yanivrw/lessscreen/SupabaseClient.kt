package com.yanivrw.lessscreen

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

val supabase by lazy {
    createSupabaseClient(SupabaseConfig.URL, SupabaseConfig.ANON_KEY) {
        install(Auth)
        install(Postgrest)
    }
}
