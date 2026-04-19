package com.example.echosync.data.repository

import android.content.Context
import com.example.echosync.data.datasource.SupabaseClientProvider
import com.example.echosync.data.models.SyncEvent
import com.example.echosync.utils.PreferencesManager
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.postgresChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Supabase Realtime 기반 방 동기화 저장소
 * - 방 생성/참가/퇴장
 * - 이벤트 발행 (PLAY, PAUSE, SEEK)
 * - 이벤트 구독 (참가자용)
 */
class SyncRepository(
    private val context: Context,
    private val prefs: PreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentRoomCode: String? = null
    private var isSubscribed = false
    
    // 참가자가 이벤트를 수신할 Flow
    private val _eventFlow = MutableSharedFlow<SyncEvent>()
    val eventFlow: SharedFlow<SyncEvent> = _eventFlow

    /**
     * 방 생성 (방장)
     * @param roomCode 생성할 방 코드
     * @return 성공 여부
     */
    suspend fun hostRoom(roomCode: String): Boolean {
        return try {
            val supabase = SupabaseClientProvider.getInstance(context)
            val userId = SupabaseClientProvider.getCurrentUserId() ?: return false
            
            // rooms 테이블에 방 정보 삽입 (RLS 정책 필요)
            supabase.postgrest["rooms"].insert(
                mapOf(
                    "room_code" to roomCode,
                    "host_id" to userId,
                    "created_at" to System.currentTimeMillis(),
                    "is_active" to true
                )
            )
            currentRoomCode = roomCode
            subscribeToRoom(roomCode) // 방장도 이벤트 수신 (필요 시)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 방 참가 (참가자)
     * @param roomCode 참가할 방 코드
     * @return 성공 여부
     */
    suspend fun joinRoom(roomCode: String): Boolean {
        return try {
            val supabase = SupabaseClientProvider.getInstance(context)
            // 방 존재 여부 확인
            val result = supabase.postgrest["rooms"]
                .select { filter("room_code", eq(roomCode)) }
                .decodeList<Map<String, Any>>()
            if (result.isEmpty()) return false
            
            currentRoomCode = roomCode
            subscribeToRoom(roomCode)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 방 퇴장 및 Realtime 구독 해제
     */
    suspend fun leaveRoom() {
        currentRoomCode?.let { roomCode ->
            try {
                val supabase = SupabaseClientProvider.getInstance(context)
                supabase.realtime.removeChannel("room-$roomCode")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        currentRoomCode = null
        isSubscribed = false
    }

    /**
     * 특정 방의 Realtime 채널 구독 (이벤트 수신)
     */
    private suspend fun subscribeToRoom(roomCode: String) {
        if (isSubscribed) return
        val supabase = SupabaseClientProvider.getInstance(context)
        val channel = supabase.realtime.channel("room-$roomCode") {
            postgresChange<SyncEvent>(schema = "public", table = "sync_events") {
                // 'sync_events' 테이블에 삽입된 이벤트를 수신
                if (it.record.roomCode == roomCode) {
                    scope.launch {
                        _eventFlow.emit(it.record)
                    }
                }
            }
        }
        channel.subscribe()
        isSubscribed = true
    }

    /**
     * 재생 이벤트 발행 (방장 전용)
     */
    suspend fun sendPlayEvent(roomCode: String, position: Long) {
        sendEvent(roomCode, SyncEvent.EventType.PLAY, position)
    }

    /**
     * 일시정지 이벤트 발행
     */
    suspend fun sendPauseEvent(roomCode: String, position: Long) {
        sendEvent(roomCode, SyncEvent.EventType.PAUSE, position)
    }

    /**
     * Seek 이벤트 발행
     */
    suspend fun sendSeekEvent(roomCode: String, position: Long) {
        sendEvent(roomCode, SyncEvent.EventType.SEEK, position)
    }

    /**
     * 공통 이벤트 발행 로직 (Supabase 'sync_events' 테이블에 INSERT)
     */
    private suspend fun sendEvent(roomCode: String, type: SyncEvent.EventType, position: Long) {
        val userId = SupabaseClientProvider.getCurrentUserId() ?: return
        val event = SyncEvent(
            roomCode = roomCode,
            type = type,
            position = position,
            timestamp = System.currentTimeMillis(),
            senderId = userId
        )
        try {
            val supabase = SupabaseClientProvider.getInstance(context)
            supabase.postgrest["sync_events"].insert(event)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}