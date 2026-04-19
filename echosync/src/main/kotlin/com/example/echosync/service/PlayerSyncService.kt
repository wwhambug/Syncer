package com.example.echosync.service

import android.content.Context
import com.example.echosync.data.models.SyncEvent
import com.example.echosync.data.repository.SyncRepository
import com.example.echosync.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 참가자 로컬 플레이어 동기화 서비스
 * - SyncRepository에서 이벤트 수신
 * - 수신된 이벤트(PLAY/PAUSE/SEEK)를 Echo Nightly 플레이어에 적용
 * 
 * ⚠️ Echo Nightly 플레이어 제어 API가 불명확하여, 실제 구현 시 조정 필요
 *   (현재는 MediaSessionManager 또는 Echo 내부 API 가정)
 */
class PlayerSyncService(
    private val context: Context,
    private val syncRepository: SyncRepository,
    private val preferencesManager: PreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isCollecting = false

    /**
     * 동기화 시작 (참가자 전용)
     * - 방에 참가 중이고 방장이 아닐 때 호출
     */
    fun startSync() {
        if (isCollecting) return
        if (preferencesManager.isRoomHost()) return // 방장은 수신 불필요
        if (preferencesManager.getCurrentRoomCode() == null) return

        isCollecting = true
        scope.launch {
            syncRepository.eventFlow.collectLatest { event ->
                applyEventToLocalPlayer(event)
            }
        }
    }

    /**
     * 동기화 중지
     */
    fun stopSync() {
        isCollecting = false
        scope.coroutineContext.cancelChildren()
    }

    /**
     * 수신된 이벤트를 로컬 플레이어에 적용
     */
    private fun applyEventToLocalPlayer(event: SyncEvent) {
        // 자신이 보낸 이벤트는 무시 (에코 방지)
        val currentUserId = getCurrentUserId() ?: return
        if (event.senderId == currentUserId) return

        when (event.type) {
            SyncEvent.EventType.PLAY -> {
                // 재생 이벤트: seek 후 재생
                seekToPosition(event.position)
                play()
            }
            SyncEvent.EventType.PAUSE -> {
                // 일시정지 이벤트
                pause()
            }
            SyncEvent.EventType.SEEK -> {
                // Seek 이벤트: 위치만 이동 (재생 상태 유지)
                seekToPosition(event.position)
            }
        }
    }

    /**
     * 로컬 플레이어 재생
     * ⚠️ Echo Nightly 플레이어 제어 방식 확인 필요
     */
    private fun play() {
        // 방법 1: MediaSessionManager 사용 (가장 가능성 높음)
        try {
            val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE)
            // 실제 구현: mediaSessionManager.dispatchMediaButtonEvent(...)
            // 또는 Echo 내부 PlayerController 호출
        } catch (e: Exception) {
            // 방법 2: Broadcast 전송 (Echo가 리시버 제공 시)
            // val intent = Intent("com.innoxstries.echo.PLAY")
            // context.sendBroadcast(intent)
        }
    }

    /**
     * 로컬 플레이어 일시정지
     */
    private fun pause() {
        // play()와 유사한 방식
    }

    /**
     * 로컬 플레이어 Seek
     * @param position 밀리초 단위 위치
     */
    private fun seekToPosition(position: Long) {
        // 실제 구현 필요
    }

    /**
     * 현재 사용자 ID 조회
     */
    private fun getCurrentUserId(): String? {
        // SupabaseClientProvider에서 가져오거나 Preferences에 저장
        return null // 임시
    }
}