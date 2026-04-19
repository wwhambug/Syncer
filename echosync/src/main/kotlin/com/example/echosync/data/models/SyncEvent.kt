package com.example.echosync.data.models

import kotlinx.serialization.Serializable

/**
 * 실시간 동기화 이벤트 데이터 모델
 * - 방장이 발생시킨 재생/일시정지/seek 이벤트를 Supabase Realtime으로 전송
 * - 참가자는 이벤트를 수신하여 로컬 플레이어 상태 동기화
 */
@Serializable
data class SyncEvent(
    val roomCode: String,           // 방 코드 (식별자)
    val type: EventType,            // 이벤트 타입 (PLAY, PAUSE, SEEK)
    val position: Long,             // 재생 위치 (밀리초)
    val timestamp: Long,            // 이벤트 발생 서버 시간 (밀리초, 클라이언트에서 생성)
    val senderId: String            // 발신자 ID (방장 식별, 중복 방지용)
) {
    @Serializable
    enum class EventType {
        PLAY, PAUSE, SEEK
    }
}

/**
 * 방 정보 모델 (Supabase 'rooms' 테이블 매핑)
 * RLS 정책: 방장만 업데이트 가능, 참가자는 읽기만 가능
 */
@Serializable
data class RoomInfo(
    val roomCode: String,           // PK: 방 코드
    val hostId: String,             // 방장 ID (익명 사용자 ID)
    val createdAt: Long,            // 생성 시간
    val participantCount: Int,      // 현재 참가자 수 (옵션)
    val isActive: Boolean = true    // 방 활성 상태
)

/**
 * 방 참가 요청 모델 (클라이언트 -> 서버)
 */
@Serializable
data class JoinRoomRequest(
    val roomCode: String,
    val userId: String
)

/**
 * 방 생성 응답 모델
 */
@Serializable
data class CreateRoomResponse(
    val success: Boolean,
    val roomCode: String,
    val message: String? = null
)