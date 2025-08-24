// MainActivity.kt
package com.samsung.health.mysteps.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Chat
import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.health.mysteps.domain.Permissions.PERMISSIONS
import com.samsung.health.mysteps.presentation.ui.ChatScreen
import com.samsung.health.mysteps.presentation.ui.DashboardScreen
import com.samsung.health.mysteps.presentation.ui.theme.PhoneAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var healthDataStore: HealthDataStore

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate()")

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val isChatVisible by viewModel.isChatVisible.collectAsStateWithLifecycle()
            val weekDays by viewModel.weekDays.collectAsState()
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            LaunchedEffect(state.permissionsGranted) {
                if (!state.permissionsGranted) {
                    try {
                        requestPermissions(this@MainActivity)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            PhoneAppTheme {
                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { viewModel.showChat() },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "AI 코치와 대화하기",
                                tint = Color.White
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                    DashboardScreen(
                        steps = state.steps,
                        sleepData = state.sleepData,
                        heartRateData = state.heartRateData,
                        weekDays = weekDays,
                        onDateSelected = { date -> viewModel.onDateSelected(date) }
                    )
                        }

                    if (isChatVisible) {
                        ModalBottomSheet(
                            onDismissRequest = { viewModel.hideChat() },
                            sheetState = sheetState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ChatScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissions(context: Activity) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = healthDataStore.requestPermissions(PERMISSIONS, context)
                viewModel.userAcceptedPermissions(result.containsAll(PERMISSIONS))
            } catch (e: HealthDataException) {
                viewModel.handleHealthDataException(e)
            } catch (e: Exception) {
                Log.e(TAG, "권한 요청 중 알 수 없는 오류", e)
            }
        }
    }
}