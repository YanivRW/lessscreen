package com.yanivrw.lessscreen

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

val supabase by lazy {
    createSupabaseClient(SupabaseConfig.URL, SupabaseConfig.ANON_KEY) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}
