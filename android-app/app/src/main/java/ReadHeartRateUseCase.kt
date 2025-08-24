/*
// ReadHeartRateUseCase.kt
package com.samsung.health.mysteps.domain

import com.samsung.android.sdk.health.data.AggregateResult
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.request.AggregateRequest
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.health.mysteps.data.model.HeartRateData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.samsung.android.sdk.health.data.Bundle as SdkBundle

class ReadHeartRateUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    suspend operator fun invoke(): HeartRateData? {
        val startTime = LocalDate.now().atStartOfDay()
        val endTime = startTime.plusDays(1).minusNanos(1)
        val zoneId = ZoneId.systemDefault()

        val filter = LocalTimeFilter.of(startTime, endTime)

        // 요청1: 일일 최소/최대 심박수
        val aggregateRequest = DataType.HeartRateType.AGGREGATE.requestBuilder
            .setLocalTimeFilter(filter)
            .build()

        // 요청2: 가장 최근 심박수
        val lastHeartRateRequest = DataType.HeartRateType.LAST.requestBuilder
            .setLocalTimeFilter(filter)
            .build()

        return try {
            // ▼▼▼ [수정] aggregateData 호출 시 기대하는 반환 타입을 명시적으로 지정합니다. ▼▼▼
            val aggregateResult = healthDataStore.aggregateData<SdkBundle>(aggregateRequest)
            val lastHeartRateResult = healthDataStore.aggregateData<Float>(lastHeartRateRequest)
            // ▲▲▲

            var dailyMin = 0
            var dailyMax = 0
            // ▼▼▼ [수정] 결과를 명시적으로 캐스팅하여 dataList에 접근합니다. ▼▼▼
            (aggregateResult as? AggregateResult<SdkBundle>)?.dataList?.firstOrNull()?.let { data ->
                dailyMin = data.value?.get(DataType.HeartRateType.AGGREGATE.min)
                    ?.toInt() ?: 0
                dailyMax = data.value?.get(DataType.HeartRateType.AGGREGATE.max)
                    ?.toInt() ?: 0
            }

            var lastHeartRate = 0
            var lastMeasuredTime = "N/A"
            (lastHeartRateResult as? AggregateResult<Float>)?.dataList?.firstOrNull()?.let { data ->
                lastHeartRate = data.value?.toInt() ?: 0
                val instant = Instant.ofEpochMilli(data.endTime.atZone(zoneId).toInstant().toEpochMilli())
                lastMeasuredTime = formatter.format(instant.atZone(zoneId))
            }

            if (dailyMin == 0 && dailyMax == 0 && lastHeartRate == 0) null
            else HeartRateData(lastMeasuredTime, lastHeartRate, dailyMin, dailyMax)

        } catch (e: Exception) {
            // 데이터가 없거나 오류 발생 시 null 반환
            null
        }
    }
}
*/
