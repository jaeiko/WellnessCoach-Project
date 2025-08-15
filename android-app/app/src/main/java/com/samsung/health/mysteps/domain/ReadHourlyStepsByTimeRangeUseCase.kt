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
import com.samsung.android.sdk.health.data.request.LocalTimeGroupUnit
import com.samsung.health.mysteps.data.model.GroupedData
import java.time.LocalDateTime
import javax.inject.Inject

class ReadHourlyStepsByTimeRangeUseCase @Inject constructor(
    private val readGroupedStepsByTimeRangeUseCase: ReadGroupedStepsByTimeRangeUseCase
) {
    @Throws(HealthDataException::class)
    suspend operator fun invoke(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): ArrayList<GroupedData> {
        return readGroupedStepsByTimeRangeUseCase(startDate, endDate, LocalTimeGroupUnit.HOURLY)
    }
}