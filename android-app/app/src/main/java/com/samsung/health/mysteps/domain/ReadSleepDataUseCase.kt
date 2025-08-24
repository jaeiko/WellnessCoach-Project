// ReadSleepDataUseCase.kt
package com.samsung.health.mysteps.domain

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.entries.SleepSession
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.health.mysteps.data.model.SleepData //mysteps의 SleepData를 사용
import com.samsung.health.mysteps.data.model.SleepStage //mysteps의 SleepStage를 사용
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ReadSleepDataUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {
    private val TAG = "ReadSleepDataUseCase"

    suspend operator fun invoke(date: LocalDate): SleepData? {
        val yesterday = LocalDate.now().minusDays(1)
        val startTime = yesterday.atTime(12, 0) // 어제 정오
        val endTime = LocalDate.now().atTime(11, 59) // 오늘 정오

        val timeFilter = LocalTimeFilter.of(startTime, endTime)

        // 튜토리얼의 prepareReadSleepRequest 로직을 여기에 통합
        val request = DataTypes.SLEEP.readDataRequestBuilder
            .setLocalTimeFilter(timeFilter)
            .build()

        return try {
            // 튜토리얼의 readSleep 로직을 여기에 통합
            val result = healthDataStore.readData(request)

            val sleepSession = result.dataList.firstOrNull() ?: run {
                Log.d(TAG, "수면 데이터가 없습니다.")
                return null
            }

            // 튜토리얼의 prepareSleepResult 로직을 여기에 통합
            val duration = sleepSession.getValue(DataType.SleepType.DURATION) ?: Duration.ZERO
            val totalMinutes = duration.toMinutes().toInt()

            val sdkSleepSessions = sleepSession.getValue(DataType.SleepType.SESSIONS) ?: emptyList()
            val stages = extractStages(sdkSleepSessions)

            Log.d(TAG, "수면 데이터 읽기 성공: 총 ${totalMinutes}분, 단계 ${stages.size}개")
            SleepData(totalMinutes, stages)
        } catch (e: Exception) {
            Log.e(TAG, "수면 데이터 읽기 중 오류 발생", e)
            null
        }
    }

    // 튜토리얼의 extractStages 로직을 여기에 통합
    private fun extractStages(sdkSessions: List<SleepSession>): List<SleepStage> {
        // 모든 세션의 모든 스테이지를 하나의 리스트로 합칩니다.
        return sdkSessions.flatMap { session ->
            session.stages?.map { sdkStage ->
                val stageName = when (sdkStage.stage) {
                    DataType.SleepType.StageType.AWAKE -> "AWAKE"
                    DataType.SleepType.StageType.LIGHT -> "LIGHT"
                    DataType.SleepType.StageType.DEEP -> "DEEP"
                    DataType.SleepType.StageType.REM -> "REM"
                    else -> "UNKNOWN"
                }
                val duration = ChronoUnit.MINUTES.between(sdkStage.startTime, sdkStage.endTime).toInt()
                SleepStage(stageName, duration)
            } ?: emptyList()
        }
    }
}