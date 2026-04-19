package com.example.echosync.di

import android.content.Context
import com.example.echosync.data.datasource.SupabaseClientProvider
import com.example.echosync.data.repository.SyncRepository
import com.example.echosync.service.PlayerSyncService
import com.example.echosync.utils.DefaultDispatcherProvider
import com.example.echosync.utils.DispatcherProvider
import com.example.echosync.utils.PreferencesManager

/**
 * 의존성 주입 모듈 (수동 DI)
 * - Koin, Dagger 없이 간단한 싱글톤 제공
 * - 필요한 객체를 한 곳에서 생성 및 관리
 */
object ExtensionModule {

    // 싱글톤 인스턴스들
    private var preferencesManager: PreferencesManager? = null
    private var syncRepository: SyncRepository? = null
    private var playerSyncService: PlayerSyncService? = null
    private var dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()

    /**
     * PreferencesManager 초기화 및 반환
     */
    fun providePreferencesManager(context: Context): PreferencesManager {
        return preferencesManager ?: synchronized(this) {
            preferencesManager ?: PreferencesManager(context).also { preferencesManager = it }
        }
    }

    /**
     * SyncRepository 초기화 및 반환
     * SupabaseClientProvider의 인증을 함께 초기화 (suspend 함수이므로 호출 측에서 코루틴 필요)
     */
    suspend fun provideSyncRepository(context: Context): SyncRepository {
        return syncRepository ?: synchronized(this) {
            syncRepository ?: run {
                // Supabase 클라이언트 및 익명 인증 트리거
                SupabaseClientProvider.getInstance(context)
                val prefs = providePreferencesManager(context)
                SyncRepository(context, prefs).also { syncRepository = it }
            }
        }
    }

    /**
     * PlayerSyncService 초기화 및 반환
     */
    fun providePlayerSyncService(context: Context): PlayerSyncService {
        return playerSyncService ?: synchronized(this) {
            playerSyncService ?: run {
                val prefs = providePreferencesManager(context)
                // 주의: syncRepository는 suspend 함수이므로 호출 시점에 코루틴 필요
                // 지금은 null로 두고, 실제 사용 시 provideSyncRepository를 먼저 호출해야 함
                PlayerSyncService(context, syncRepository!!, prefs).also { playerSyncService = it }
            }
        }
    }

    /**
     * DispatcherProvider 반환 (테스트용 교체 가능)
     */
    fun provideDispatcherProvider(): DispatcherProvider = dispatcherProvider

    /**
     * 테스트용 DispatcherProvider 교체
     */
    fun setDispatcherProvider(provider: DispatcherProvider) {
        dispatcherProvider = provider
    }

    /**
     * 모든 싱글톤 초기화 해제 (익스텐션 종료 시 호출)
     */
    fun clear() {
        preferencesManager = null
        syncRepository?.leaveRoom()
        syncRepository = null
        playerSyncService = null
        SupabaseClientProvider.cleanup()
    }
}