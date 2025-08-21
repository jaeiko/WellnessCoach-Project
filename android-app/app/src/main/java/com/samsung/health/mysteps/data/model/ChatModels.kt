package com.samsung.health.mysteps.data.model

// 서버로 보낼 요청 모델
data class ChatRequest(
    val userId: String,
    val sessionId: String,
    val message: String
)

// 서버로부터 받을 응답 모델
data class ChatResponse(
    val chatResponse: String
    // notification 필드는 필요 시 추가
)