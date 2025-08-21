// SleepData.kt
package com.samsung.health.mysteps.data.model

/**
 * 수면 데이터를 담기 위한 데이터 클래스
 *
 * @property totalSleepMinutes 총 수면 시간 (분)
 * @property sleepStages 각 수면 단계별 시간(분)을 담는 맵. 예: {"DEEP": 80, "REM": 120}
 */
data class SleepData(
    val totalSleepMinutes: Int,
    val sleepStages: Map<String, Int>
)