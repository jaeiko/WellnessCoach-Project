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

// AI가 생성하는 JSON 전체 구조에 대응하는 데이터 클래스
data class AiAnalysisResponse(
    val analysis_json: Map<String, Any>, // 상세 분석 내용은 Map으로 받습니다.
    val response_for_user: String      // 사용자에게 보여줄 실제 텍스트입니다.
)