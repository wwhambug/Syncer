package com.example.echosync.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 로컬 설정 및 방 정보 저장소
 * - 방 코드, 방장 여부 저장
 * - SharedPreferences 사용 (간단한 키-값)
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "echo_sync_prefs"
        private const val KEY_ROOM_CODE = "room_code"
        private const val KEY_IS_HOST = "is_host"
    }

    /**
     * 현재 참가 중인 방 코드 저장
     */
    suspend fun saveRoomInfo(roomCode: String, isHost: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putString(KEY_ROOM_CODE, roomCode)
            putBoolean(KEY_IS_HOST, isHost)
        }.apply()
    }

    /**
     * 방 정보 초기화 (퇴장 시)
     */
    suspend fun clearRoomInfo() = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            remove(KEY_ROOM_CODE)
            remove(KEY_IS_HOST)
        }.apply()
    }

    /**
     * 현재 방 코드 반환 (없으면 null)
     */
    fun getCurrentRoomCode(): String? = prefs.getString(KEY_ROOM_CODE, null)

    /**
     * 현재 방장 여부 반환 (방 정보 없으면 false)
     */
    fun isRoomHost(): Boolean = prefs.getBoolean(KEY_IS_HOST, false)

    /**
     * 방에 참가 중인지 여부
     */
    fun isInRoom(): Boolean = getCurrentRoomCode() != null
}