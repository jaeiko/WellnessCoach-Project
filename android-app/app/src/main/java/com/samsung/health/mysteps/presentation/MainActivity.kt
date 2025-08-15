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
package com.samsung.health.mysteps.presentation

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import com.samsung.health.mysteps.domain.Permissions.PERMISSIONS
import com.samsung.health.mysteps.presentation.ui.Main
import com.samsung.health.mysteps.presentation.ui.theme.PhoneAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var healthDataStore: HealthDataStore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate()")
        val activityContext = this
        setContent {
            var errorDescription = ""
            var errorCode = ""
            val state by viewModel.state.collectAsStateWithLifecycle()
            val refresh = state.refresh
            state.errorLevel?.let { healthError ->
                if (healthError.resolvable) {
                    (healthError.error as ResolvablePlatformException).resolve(activityContext)
                }
                errorDescription = healthError.description
                errorCode = healthError.code
            }
            Log.i(TAG, "state.refresh: $refresh")
            if (state.errorLevel == null) {
                if (state.permissionRequested && !state.permissionsGranted) {
                    try {
                        requestPermissions(activityContext)
                    } catch (cancellationException: CancellationException) {
                        cancellationException.printStackTrace()
                    }
                }
            }

            PhoneAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Main(
                        state.steps,
                        refresh,
                        state.permissionsGranted,
                        errorCode,
                        errorDescription,
                        { requestPermissions(activityContext) },
                        { if (refresh) viewModel.readSteps(); else viewModel.refresh() })
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun requestPermissions(context: Activity) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = healthDataStore.requestPermissions(PERMISSIONS, context)
                viewModel.userAcceptedPermissions(result.containsAll(PERMISSIONS))
            } catch (healthDataException: HealthDataException) {
                viewModel.handleHealthDataException(healthDataException)
            } catch (cancellationException: CancellationException) {
                Log.i(TAG, cancellationException.message.toString())
            }
        }
    }
}
