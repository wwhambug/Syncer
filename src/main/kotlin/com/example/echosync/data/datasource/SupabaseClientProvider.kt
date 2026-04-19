package com.example.echosync.data.datasource

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Anonymous
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

/**
 * Supabase 클라이언트 싱글톤 제공자
 * - 익명 인증 자동 처리
 * - Realtime 채널 관리를 위한 클라이언트 초기화
 */
object SupabaseClientProvider {

    private const val SUPABASE_URL = "https://vtqseocgkzcfwaestckw.supabase.co"
    private const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_1NLq5I6a_hvxwqxqZp1RFA_5F_XVMMT"

    private var client: SupabaseClient? = null
    private var currentUserId: String? = null
    private val initMutex = Any()

    /**
     * Supabase 클라이언트 초기화 및 익명 인증
     * @param context Android Context (필요 시 사용, 현재는 API 키만으로 초기화 가능)
     * @return 초기화된 SupabaseClient 인스턴스
     */
    suspend fun getInstance(context: Context): SupabaseClient {
        synchronized(initMutex) {
            if (client == null) {
                client = createSupabaseClient(
                    supabaseUrl = SUPABASE_URL,
                    supabaseKey = SUPABASE_PUBLISHABLE_KEY
                ) {
                    install(Postgrest)
                    install(Auth)
                    install(Realtime) {
                        config = RealtimeConfig(
                            json = Json { ignoreUnknownKeys = true }
                        )
                    }
                }
                // 익명 로그인 수행
                performAnonymousLogin()
            }
        }
        return client!!
    }

    /**
     * 익명 사용자 로그인 수행 및 userId 캐싱
     */
    private suspend fun performAnonymousLogin() {
        val supabase = client ?: return
        try {
            val session = supabase.auth.signInWith(Anonymous)
            currentUserId = session.user.id
            android.util.Log.d("SupabaseClient", "Anonymous login success, userId: $currentUserId")
        } catch (e: Exception) {
            android.util.Log.e("SupabaseClient", "Anonymous login failed", e)
            throw e
        }
    }

    /**
     * 현재 익명 사용자 ID 반환 (인증 완료 후 호출 필요)
     */
    fun getCurrentUserId(): String? = currentUserId

    /**
     * 클라이언트 리소스 정리 (필요 시)
     */
    fun cleanup() {
        client?.realtime?.destroy()
        client = null
        currentUserId = null
    }
}