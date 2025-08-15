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

import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.request.AggregateRequest
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeGroup
import com.samsung.android.sdk.health.data.request.LocalTimeGroupUnit
import com.samsung.health.mysteps.data.model.GroupedData
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class ReadGroupedStepsByTimeRangeUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {
    @Throws(HealthDataException::class)
    suspend operator fun invoke(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        localTimeGroupUnit: LocalTimeGroupUnit
    ): ArrayList<GroupedData> {
        return readStepsByTimeRange(startTime, endTime, localTimeGroupUnit)
    }

    @Throws(HealthDataException::class)
    private suspend fun readStepsByTimeRange(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        localTimeGroupUnit: LocalTimeGroupUnit
    ): ArrayList<GroupedData> {
        val multiplier = 1

        val aggregateRequest =
            getAggregateRequestBuilder(startDateTime, endDateTime, localTimeGroupUnit, multiplier)
        val result = healthDataStore.aggregateData(aggregateRequest)

        val stepList: ArrayList<GroupedData> = ArrayList()
        result.dataList.forEach { aggregatedData ->
            var stepCount = 0L
            aggregatedData.value?.let { stepCount = it }
            val startTime = aggregatedData.startTime.atZone(ZoneId.systemDefault())
            val groupedData = GroupedData(stepCount, startTime.toLocalDateTime())
            stepList.add(groupedData)
        }
        return stepList
    }

    @Throws(HealthDataException::class)
    fun getAggregateRequestBuilder(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        localTimeGroupUnit: LocalTimeGroupUnit,
        multiplier: Int
    ): AggregateRequest<Long> {
        val localTimeFilter = LocalTimeFilter.of(startDateTime, endDateTime)
        val localTimeGroup = LocalTimeGroup.of(localTimeGroupUnit, multiplier)
        val aggregateRequest = DataType.StepsType.TOTAL.requestBuilder
            .setLocalTimeFilterWithGroup(localTimeFilter, localTimeGroup)
            .build()
        return aggregateRequest
    }
}