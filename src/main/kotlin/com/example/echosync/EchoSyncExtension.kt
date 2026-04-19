package com.example.echosync

import android.content.Context
import android.content.Intent
import com.example.echosync.data.repository.SyncRepository
import com.example.echosync.utils.PreferencesManager
import com.innoxstries.echoapi.extension.TrackerExtension
import com.innoxstries.echoapi.model.Track
import kotlinx.coroutines.*

/**
 * Echo Nightly v725 실시간 음악 동기화 익스텐션
 * - 방장인 경우 재생/일시정지/seek 이벤트를 감지하여 Supabase Realtime으로 발행
 * - 참가자인 경우 이벤트 수신 후 로컬 플레이어 제어는 별도 서비스에서 처리
 */
class EchoSyncExtension : TrackerExtension() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var syncRepository: SyncRepository
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(context: Context) {
        super.onCreate(context)
        // 초기화: PreferencesManager, SyncRepository (의존성 직접 생성, DI 생략)
        preferencesManager = PreferencesManager(context)
        syncRepository = SyncRepository(context, preferencesManager)

        // 방에 이미 참가 중이었다면 Realtime 채널 재구독
        val roomCode = preferencesManager.getCurrentRoomCode()
        if (roomCode != null && preferencesManager.isRoomHost()) {
            scope.launch {
                syncRepository.hostRoom(roomCode)
            }
        } else if (roomCode != null && !preferencesManager.isRoomHost()) {
            scope.launch {
                syncRepository.joinRoom(roomCode)
            }
        }
    }

    override fun onPlay(track: Track, position: Long) {
        // 방장일 때만 재생 이벤트 전송
        if (preferencesManager.isRoomHost()) {
            val roomCode = preferencesManager.getCurrentRoomCode() ?: return
            scope.launch {
                syncRepository.sendPlayEvent(roomCode, position)
            }
        }
        // 참가자는 이벤트 수신 대기만 함 (PlayerSyncService에서 처리)
    }

    override fun onPause(track: Track, position: Long) {
        if (preferencesManager.isRoomHost()) {
            val roomCode = preferencesManager.getCurrentRoomCode() ?: return
            scope.launch {
                syncRepository.sendPauseEvent(roomCode, position)
            }
        }
    }

    override fun onSeek(track: Track, position: Long) {
        if (preferencesManager.isRoomHost()) {
            val roomCode = preferencesManager.getCurrentRoomCode() ?: return
            scope.launch {
                syncRepository.sendSeekEvent(roomCode, position)
            }
        }
    }

    override fun onSettingsClicked(context: Context) {
        // Extension Settings API 호출: SyncSettingsActivity 실행
        val intent = Intent(context, SyncSettingsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel() // 코루틴 정리
        syncRepository.leaveRoom() // 방 퇴장 및 Realtime 채널 해제
    }
}