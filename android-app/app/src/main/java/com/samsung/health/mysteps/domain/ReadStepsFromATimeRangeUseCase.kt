/*
 * Copyright 2025 Samsung Electronics Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samsung.health.mysteps.domain

import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.AggregateRequest
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import java.time.LocalDateTime
import javax.inject.Inject

private const val TAG = "ReadStepsFromATimeRangeUseCase"

class ReadStepsFromATimeRangeUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {
    @Throws(HealthDataException::class)
    suspend operator fun invoke(startDateTime: LocalDateTime, endDateTime: LocalDateTime): Long {
        Log.i(TAG, "invoke()")
        return readSteps(startDateTime, endDateTime)
    }

    @Throws(HealthDataException::class)
    private suspend fun readSteps(
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Long {
        val aggregateRequest = getAggregateRequestBuilder(startTime, endTime)
        val result = healthDataStore.aggregateData(aggregateRequest)
        var stepCount = 0L
        result.dataList.forEach { aggregatedData ->
            aggregatedData.value?.let { stepCount = it }
        }
        return stepCount
    }

    @Throws(HealthDataException::class)
    fun getAggregateRequestBuilder(
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): AggregateRequest<Long> {
        val localTimeFilter = LocalTimeFilter.of(startTime, endTime)
        val aggregateRequest = DataType.StepsType.TOTAL.requestBuilder
            .setLocalTimeFilter(localTimeFilter)
            .build()
        return aggregateRequest
    }
}
