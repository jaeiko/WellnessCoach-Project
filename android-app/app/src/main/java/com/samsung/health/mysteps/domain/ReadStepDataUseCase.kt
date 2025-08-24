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

import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.health.mysteps.data.model.StepData
import com.samsung.health.mysteps.data.model.SleepData
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

class ReadStepDataUseCase @Inject constructor(

    private val readStepGoalFromTodayUseCase: ReadStepGoalFromTodayUseCase,
    private val readStepsFromATimeRangeUseCase: ReadStepsFromATimeRangeUseCase,
    private val readHourlyStepsByTimeRangeUseCase: ReadHourlyStepsByTimeRangeUseCase

) {
    @Throws(HealthDataException::class)
    suspend operator fun invoke(date: LocalDate): StepData {
        val startTime = date.atStartOfDay()
        val endTime = startTime.plusDays(1).minusNanos(1)
        val stepGoal = readStepGoalFromTodayUseCase()
        val stepCount = readStepsFromATimeRangeUseCase(startTime, endTime)
        val stepsByHour = readHourlyStepsByTimeRangeUseCase(startTime, endTime)

        return StepData(stepCount, stepGoal, stepsByHour)
    }
}
