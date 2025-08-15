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
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "ReadStepGoalFromTodayUseCase"

class ReadStepGoalFromTodayUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {
    @Throws(HealthDataException::class)
    suspend operator fun invoke(): Int {
        return readLastStepGoal()
    }

    @Throws(HealthDataException::class)
    private suspend fun readLastStepGoal(): Int {
        val startDate = LocalDate.now()
        val endDate = LocalDate.now().plusDays(1)
        Log.i(TAG, "StartDate: $startDate; EndDate: $endDate")
        val readRequest = DataType.StepsGoalType.LAST
            .requestBuilder
            .setLocalDateFilter(LocalDateFilter.of(startDate, endDate))
            .build()
        val result = healthDataStore.aggregateData(readRequest)
        var stepGoal = 0
        result.dataList.forEach { data ->
            Log.i(TAG, "Step Goal: ${data.value}")

            Log.i(TAG, "data.startTime: ${data.startTime}")
            Log.i(TAG, "data.endTime: ${data.endTime}")
            data.value?.let { stepGoal = it }
        }
        return stepGoal
    }
}
