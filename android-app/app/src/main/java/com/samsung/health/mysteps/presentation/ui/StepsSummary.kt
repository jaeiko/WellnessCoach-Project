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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.health.mysteps.R

@Composable
fun StepsFromTodayCard(
    stepCount: Long,
    stepGoal: Int
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val stepProgress = if (stepGoal > 0) (stepCount.toFloat() / stepGoal.toFloat()) else 0f
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle()) {
                    append("%,d".format(stepCount))
                }
                withStyle(style = SpanStyle(fontSize = 24.sp)) {
                    append(" ${stringResource(id = R.string.summary_text_steps)}")
                }
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(40.dp, 54.dp, 40.dp, 0.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(13.dp))

        CustomLinearProgressIndicator(
            modifier = Modifier
                .padding(40.dp, 10.dp, 40.dp, 7.dp)
                .fillMaxWidth()
                .height(42.dp),
            progress = stepProgress,
            roundedCornerSize = 22.dp,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
        )
        val description = "${stringResource(id = R.string.text_Target)} %,d".format(stepGoal)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(45.dp, 0.dp, 45.dp, 0.dp)
        ) {
            Text(
                style = MaterialTheme.typography.labelSmall,
                text = "0",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.CenterStart)
            )
            Text(
                style = MaterialTheme.typography.labelSmall,
                text = description,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}