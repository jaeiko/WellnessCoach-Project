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
package com.samsung.health.mysteps.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samsung.health.mysteps.R
import com.samsung.health.mysteps.data.model.StepData
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun Content(
    steps: StepData
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(38.dp))
        val dateOfToday =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d")).toString()
        val lastUpdateTime =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, h:mm a")).toString()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 0.dp, 20.dp, 0.dp)
        ) {
            Text(
                modifier = Modifier.align(Alignment.CenterStart),
                style = MaterialTheme.typography.titleLarge,
                text = dateOfToday,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                modifier = Modifier.align(Alignment.BottomEnd),
                style = MaterialTheme.typography.labelSmall,
                text = stringResource(R.string.last_updated_time, lastUpdateTime),
                color = MaterialTheme.colorScheme.onSecondary,
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StepsFromTodayCard(steps.count, steps.goal)
        }
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp)
                .clip(shape = RoundedCornerShape(26.dp))
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            StepsByHourList(steps.hourly)
        }
    }
}