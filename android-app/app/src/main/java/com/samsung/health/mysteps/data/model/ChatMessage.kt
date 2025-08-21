package com.samsung.health.mysteps.data.model

// 메시지를 보낸 주체 (사용자인지, AI 모델인지)
enum class Sender {
    USER, MODEL
}

// 채팅 메시지를 나타내는 데이터 클래스
data class ChatMessage(
    val text: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis() // 메시지 시간
)