package com.binaryxbeast.krewzo.core.network

import com.binaryxbeast.krewzo.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object KrewzoSupabaseClient {
    val sanitizedUrl: String by lazy {
        BuildConfig.SUPABASE_URL
            .trim()
            .removeSuffix("/rest/v1")
            .removeSuffix("/rest/v1/")
            .removeSuffix("/")
    }

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = sanitizedUrl,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        ) {
            install(Postgrest)
            install(Auth)
            install(Realtime)
            install(Storage)
        }
    }
}
