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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.health.mysteps.R
import com.samsung.health.mysteps.data.model.GroupedData
import com.samsung.health.mysteps.data.model.StepData
import com.samsung.health.mysteps.presentation.ui.theme.PhoneAppTheme
import java.time.LocalDateTime

private const val TAG = "Main"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(
    steps: StepData,
    refreshState: Boolean,
    permissionsGranted: Boolean,
    errorCode: String,
    errorDescription: String,
    permissionsInvoked: () -> Unit,
    refresh: () -> Unit,
) {
    Log.i(TAG, "refreshState: $refreshState")
    val state = remember {
        object : PullToRefreshState {
            private val anim = Animatable(0f, Float.VectorConverter)
            override val distanceFraction
                get() = anim.value

            override suspend fun animateToThreshold() {
                anim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }

            override suspend fun animateToHidden() {
                anim.animateTo(0f)
            }

            override suspend fun snapTo(targetValue: Float) {
                anim.snapTo(targetValue)
            }
        }
    }
    if (refreshState) refresh()
    PullToRefreshBox(
        isRefreshing = refreshState,
        onRefresh = refresh,
        state = state
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .align(Alignment.TopCenter)
        ) {
            item {
                Spacer(Modifier.height(26.dp))
            }
            item {
                TopBar { permissionsInvoked() }
            }
            item {
                if (permissionsGranted && errorDescription == "") {
                    Content(steps)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(30.dp, 60.dp, 30.dp, 20.dp)
                    ) {
                        Row(modifier = Modifier.height(500.dp)) {
                            var errorResolution = ""
                            Errors.resolutionMap[errorCode]?.let {
                                errorResolution = stringResource(it)
                            }
                            val errorMessage = if (errorDescription == "") {
                                stringResource(id = R.string.no_permission_text)
                            } else {
                                "\n\n$errorDescription\n\nerror_code: $errorCode"
                            } + "\n" + stringResource(id = R.string.refer_to_doc)
                            Text(
                                buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontSize = 26.sp)) {
                                        append(stringResource(id = R.string.oops_i_did_it_again) + "\n")
                                    }
                                    withStyle(SpanStyle()) {
                                        append(errorResolution)
                                    }
                                    withStyle(SpanStyle(fontSize = 15.sp, color = Color.DarkGray)) {
                                        append(errorMessage)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.pull_down_to_refresh),
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            textAlign = TextAlign.Center,

                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }
    }
}

object Errors {
    val resolutionMap: Map<String, Int> = mapOf(
        "2000" to R.string.error_resolution_2000,
        "2003" to R.string.error_resolution_2003,
        "2004" to R.string.error_resolution_2004,

        "3000" to R.string.error_resolution_3000,
        "3001" to R.string.error_resolution_3001,
        "3002" to R.string.error_resolution_3002,
        "3003" to R.string.error_resolution_3003,
        "3004" to R.string.error_resolution_3004,

        "9000" to R.string.error_resolution_platform_errors,
        "9001" to R.string.error_resolution_platform_errors,
        "9002" to R.string.error_resolution_platform_errors,
        "9003" to R.string.error_resolution_platform_errors,
        "9004" to R.string.error_resolution_platform_errors,
        "9005" to R.string.error_resolution_platform_errors,
        "9006" to R.string.error_resolution_platform_errors
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun Preview() {
    PhoneAppTheme {
        Main(
            StepData(
                1342, 3000, arrayListOf(
                    GroupedData(100, LocalDateTime.now()),
                    GroupedData(2340, LocalDateTime.now()),
                    GroupedData(2300, LocalDateTime.now()),
                )
            ), refreshState = true, permissionsGranted = true, "", "", {}, {})
    }
}