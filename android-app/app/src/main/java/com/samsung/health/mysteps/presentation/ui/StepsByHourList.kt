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

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.samsung.health.mysteps.data.model.GroupedData
import java.time.format.DateTimeFormatter

private const val TAG = "StepsByHourList"

@Composable
fun StepsByHourList(
    stepList: ArrayList<GroupedData>
) {
    Column(
        modifier = Modifier
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(0.dp, 5.dp, 0.dp, 12.dp)
                .fillMaxWidth()
        ) {
            Text(
                style = MaterialTheme.typography.titleSmall,
                text = stringResource(id = R.string.steps_by_hour),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        val maxGroupedStepCount = stepList.maxByOrNull { it.count }?.count ?: 0
        Log.i(TAG, "maxGroupedStepCount: $maxGroupedStepCount")
        stepList.forEach {
            OneItem(it, maxGroupedStepCount)
        }
    }
}

@Composable
fun OneItem(hourlyStepItem: GroupedData, maxGroupedStepCount: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(10.dp))
            .background(color = MaterialTheme.colorScheme.primaryContainer)
            .padding(20.dp)
    ) {
        val startHour =
            hourlyStepItem.startTime.format(DateTimeFormatter.ofPattern("h a")).toString()
        // we calculate the end time of 1 hour span by adding 1 to the start hour
        val endHour =
            hourlyStepItem.startTime.plusHours(1).format(DateTimeFormatter.ofPattern("h a"))
                .toString()
        val timeString = "$startHour - $endHour"
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp, 4.dp, 0.dp, 0.dp)
        ) {
            Text(
                modifier = Modifier.align(Alignment.CenterStart),
                style = MaterialTheme.typography.labelMedium,
                text = timeString,
                color = MaterialTheme.colorScheme.onSecondary,
            )
            Text(
                modifier = Modifier.align(Alignment.CenterEnd),
                style = MaterialTheme.typography.labelMedium,
                text = "${"%,d".format(hourlyStepItem.count)} ${stringResource(id = R.string.summary_text_steps)}",
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(11.dp))
        val progress =
            if (maxGroupedStepCount != 0L) hourlyStepItem.count.toFloat() / maxGroupedStepCount else 0F
        CustomLinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp)
                .height(6.dp),
            progress = progress,
            roundedCornerSize = 3.dp,
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer
        )
    }
}