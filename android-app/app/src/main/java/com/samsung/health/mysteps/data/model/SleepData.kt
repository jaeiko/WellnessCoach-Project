// SleepData.kt
package com.samsung.health.mysteps.data.model

/**
 * AI 분석 및 UI 표시에 사용할 최종 수면 데이터 형태
 * @param totalSleepMinutes 총 수면 시간 (분)
 * @param stages 각 수면 단계 정보 리스트
 */
data class SleepData(
    val totalSleepMinutes: Int,
    val stages: List<SleepStage>
)

/**
 * 수면의 각 단계를 표현하는 데이터 클래스
 * @param stage 수면 단계 이름 (AWAKE, LIGHT, DEEP, REM)
 * @param durationMinutes 해당 단계의 지속 시간 (분)
 */
data class SleepStage(
    val stage: String,
    val durationMinutes: Int
)