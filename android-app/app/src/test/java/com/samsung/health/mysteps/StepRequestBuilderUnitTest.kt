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
package com.samsung.health.mysteps

import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.health.mysteps.domain.ReadStepsFromATimeRangeUseCase
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class StepRequestBuilderUnitTest {
    private val healthDataStore: HealthDataStore = mockk(relaxed = true)
    private val readStepsFromATimeRangeUseCase = ReadStepsFromATimeRangeUseCase(healthDataStore)

    @Test
    fun aggregateRequest_isCorrect() {
        val startTime = LocalDateTime.now()
        val endTime = LocalDateTime.now()
        val aggregateRequest =
            readStepsFromATimeRangeUseCase.getAggregateRequestBuilder(startTime, endTime)

        assertEquals(startTime, aggregateRequest.localTimeFilter?.startTime)
        assertEquals(endTime, aggregateRequest.localTimeFilter?.endTime)
        assertEquals(DataType.StepsType.TOTAL, aggregateRequest.targetOperation)
    }
}