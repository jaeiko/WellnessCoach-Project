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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.sdk.health.data.error.AuthorizationException
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.InvalidRequestException
import com.samsung.android.sdk.health.data.error.PlatformInternalException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import com.samsung.health.mysteps.data.model.HealthError
import com.samsung.health.mysteps.data.model.StepData
import com.samsung.health.mysteps.domain.ArePermissionsGrantedUseCase
import com.samsung.health.mysteps.domain.ReadStepDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private const val TAG = "MainViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(
    private val arePermissionsGrantedUseCase: ArePermissionsGrantedUseCase,
    private val readStepDataUseCase: ReadStepDataUseCase
) : ViewModel() {
    init {
        Log.i(TAG, "init()")
        checkPermissions()
    }

    private val _state =
        MutableStateFlow(
            State(
                permissionRequested = false,
                permissionsGranted = false,
                steps = StepData(0, 0, ArrayList()),
                refresh = false,
                errorLevel = null,
            )
        )
    val state: StateFlow<State> = _state

    fun refresh() {
        Log.i(TAG, "refresh()")
        _state.update { currentState ->
            currentState.copy(
                refresh = true
            )
        }
    }

    private fun checkPermissions() {
        Log.i(TAG, "checkPermissions()")
        viewModelScope.launch {
            try {
                val permissionsGranted = arePermissionsGrantedUseCase()
                if (permissionsGranted) {
                    refresh()
                } else {
                    Log.i(TAG, "Permission not granted so far")
                }
                _state.update { currentState ->
                    currentState.copy(
                        permissionRequested = true,
                        permissionsGranted = permissionsGranted
                    )
                }
            } catch (healthDataException: HealthDataException) {
                handleHealthDataException(healthDataException)
            }
        }
    }

    fun readSteps() {
        Log.i(TAG, "readSteps()")
        viewModelScope.launch {
            try {
                val steps = readStepDataUseCase()
                _state.update { currentState ->
                    currentState.copy(
                        steps = steps,
                        errorLevel = null
                    )
                }
                // 🔽 [핵심 추가] 걸음 수 데이터를 성공적으로 읽어온 직후, Firebase로 전송합니다.
                sendStepsToFirebase(steps)

            } catch (healthDataException: HealthDataException) {
                handleHealthDataException(healthDataException)
            } finally {
                _state.update { currentState ->
                    currentState.copy(
                        refresh = false
                    )
                }
            }
        }
    }

    fun userAcceptedPermissions(agreed: Boolean) {
        Log.i(TAG, "userAcceptedPermissions")
        _state.update { currentState ->
            currentState.copy(
                permissionRequested = false,
                permissionsGranted = agreed
            )
        }
        refresh()
    }

    fun handleHealthDataException(healthDataException: HealthDataException) {
        val errorMessage = healthDataException.errorMessage
        val errorCode = healthDataException.errorCode ?: 0
        val healthError =
            HealthError(healthDataException, errorCode.toString(), errorMessage, false)
        if (healthDataException is ResolvablePlatformException && healthDataException.hasResolution) {
            Log.i(
                TAG,
                "Resolvable Exception; message: ${healthDataException.errorMessage}"
            )
            healthError.error = healthDataException
            healthError.resolvable = true
        } else if (healthDataException is AuthorizationException) {
            Log.i(TAG, "Authorization Exception")
        } else if (healthDataException is InvalidRequestException) {
            Log.i(TAG, "Invalid Request Exception")
        } else if (healthDataException is PlatformInternalException) {
            Log.i(TAG, "Platform Internal Exception")
        }
        _state.update { currentState ->
            currentState.copy(errorLevel = healthError)
        }
    }

    fun sendStepsToFirebase(stepsData: StepData) {
        val db = Firebase.firestore
        val userId = "user_1"
        val dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

        // 🔽 [핵심 수정] StepData.kt에 정의된 올바른 프로퍼티 이름(count, hourly)을 사용합니다.
        val totalSteps = stepsData.count
        val stepGoal = stepsData.goal
        val hourlySteps = stepsData.hourly.associate { it.startTime.hour.toString() to it.count }

        // Python AI가 이해할 수 있도록 sample_data.json과 유사한 구조로 데이터를 만듭니다.
        val exerciseData = hashMapOf(
            "exercise_type" to "STEPS_DAILY",
            "end_time" to com.google.firebase.Timestamp.now(),
            "stats" to hashMapOf(
                "total_steps" to totalSteps,
                "goal" to stepGoal, // 목표 걸음 수도 함께 전송
                "hourly_steps" to hourlySteps
            )
        )

        val healthLog = hashMapOf(
            "user_profile" to hashMapOf("user_id" to userId), // 임시 프로필 정보
            "exercise_data" to listOf(exerciseData),
            "timestamp" to FieldValue.serverTimestamp()
        )

        // Firestore에 데이터를 저장합니다.
        db.collection("users").document(userId).collection("health_logs").document(dateString)
            .set(healthLog, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { Log.d("FIRESTORE", "데이터 전송 성공!") }
            .addOnFailureListener { e -> Log.w("FIRESTORE", "데이터 전송 실패", e) }
    }
}

data class State(
    val permissionRequested: Boolean,
    val permissionsGranted: Boolean,
    val steps: StepData,
    val refresh: Boolean,
    val errorLevel: HealthError?,
)