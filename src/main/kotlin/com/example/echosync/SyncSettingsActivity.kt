package com.example.echosync

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.echosync.data.repository.SyncRepository
import com.example.echosync.utils.PreferencesManager
import kotlinx.coroutines.launch

/**
 * Extension Settings UI
 * - 방 생성: 새 방 코드 생성 후 방장으로 입장
 * - 방 참가: 기존 방 코드 입력 후 참가자로 입장
 * - 현재 방 정보 표시 및 퇴장 버튼
 */
class SyncSettingsActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var syncRepository: SyncRepository

    private lateinit var roomCodeInput: EditText
    private lateinit var joinButton: Button
    private lateinit var createButton: Button
    private lateinit var leaveButton: Button
    private lateinit var statusText: TextView
    private lateinit var currentRoomText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync_settings)

        preferencesManager = PreferencesManager(this)
        syncRepository = SyncRepository(this, preferencesManager)

        // UI 바인딩 (직접 findViewById, 실제 레이아웃 XML 필요)
        roomCodeInput = findViewById(R.id.roomCodeInput)
        joinButton = findViewById(R.id.joinButton)
        createButton = findViewById(R.id.createButton)
        leaveButton = findViewById(R.id.leaveButton)
        statusText = findViewById(R.id.statusText)
        currentRoomText = findViewById(R.id.currentRoomText)

        // 현재 참가 중인 방 정보 표시
        updateCurrentRoomInfo()

        createButton.setOnClickListener {
            lifecycleScope.launch {
                createRoom()
            }
        }

        joinButton.setOnClickListener {
            val code = roomCodeInput.text.toString().trim().uppercase()
            if (code.isNotEmpty()) {
                lifecycleScope.launch {
                    joinRoom(code)
                }
            } else {
                Toast.makeText(this, "방 코드를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

        leaveButton.setOnClickListener {
            lifecycleScope.launch {
                leaveCurrentRoom()
            }
        }
    }

    private suspend fun createRoom() {
        // 간단한 랜덤 방 코드 생성 (예: 6자리 영대문자+숫자)
        val newRoomCode = generateRoomCode()
        val success = syncRepository.hostRoom(newRoomCode)
        if (success) {
            preferencesManager.saveRoomInfo(newRoomCode, isHost = true)
            updateCurrentRoomInfo()
            Toast.makeText(this, "방 생성 완료: $newRoomCode", Toast.LENGTH_LONG).show()
            finish() // 설정 종료 (선택 사항)
        } else {
            statusText.text = "방 생성 실패 (네트워크 오류 또는 중복 코드)"
        }
    }

    private suspend fun joinRoom(roomCode: String) {
        val success = syncRepository.joinRoom(roomCode)
        if (success) {
            preferencesManager.saveRoomInfo(roomCode, isHost = false)
            updateCurrentRoomInfo()
            Toast.makeText(this, "방 참가 완료: $roomCode", Toast.LENGTH_LONG).show()
            finish()
        } else {
            statusText.text = "방 참가 실패: 방이 존재하지 않거나 풀 상태"
        }
    }

    private suspend fun leaveCurrentRoom() {
        syncRepository.leaveRoom()
        preferencesManager.clearRoomInfo()
        updateCurrentRoomInfo()
        Toast.makeText(this, "방을 퇴장했습니다", Toast.LENGTH_SHORT).show()
    }

    private fun updateCurrentRoomInfo() {
        val roomCode = preferencesManager.getCurrentRoomCode()
        if (roomCode != null) {
            val role = if (preferencesManager.isRoomHost()) "방장" else "참가자"
            currentRoomText.text = "현재 방: $roomCode ($role)"
            leaveButton.isEnabled = true
        } else {
            currentRoomText.text = "참가 중인 방 없음"
            leaveButton.isEnabled = false
        }
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}