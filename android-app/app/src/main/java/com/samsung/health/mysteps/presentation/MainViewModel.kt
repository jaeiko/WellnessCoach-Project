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


import com.samsung.health.mysteps.data.api.ChatApiService
import com.samsung.health.mysteps.data.model.ChatMessage
import com.samsung.health.mysteps.data.model.ChatRequest
import com.samsung.health.mysteps.data.model.Sender
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
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
import com.google.gson.Gson
import com.samsung.health.mysteps.data.model.AiAnalysisResponse

private const val TAG = "MainViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(
    private val arePermissionsGrantedUseCase: ArePermissionsGrantedUseCase,
    private val readStepDataUseCase: ReadStepDataUseCase,
    private val chatApiService: ChatApiService
) : ViewModel() {
    init {
        Log.i(TAG, "init()")
        checkPermissions()
    }

    private val sessionId = "session_${UUID.randomUUID()}"
    private val userId = "user_1" // 임시 사용자 ID

    // 채팅 메시지 목록 (UI가 관찰)
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = """
                    안녕하세요! 저는 당신의 건강한 삶을 돕는 AI 웰니스 코치입니다. 😊

                    걸음 수, 수면 데이터 등을 분석하여 건강 상태를 알려드리고, 맞춤형 건강 루틴을 제안해 드릴 수 있어요.

                    먼저, 저에게 이렇게 물어보시는 건 어떨까요?
                    "오늘 내 건강 데이터 분석해줘"
                """.trimIndent(), // trimIndent()를 사용하면 코드의 들여쓰기가 제거되어 깔끔하게 보입니다.
                sender = Sender.MODEL
            )
        )
    )

    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

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

    // 메시지 전송 함수
    fun sendMessage(message: String) {
        // 사용자 메시지와 로딩 메시지를 목록에 추가하는 부분 (기존과 동일)
        val userMessage = ChatMessage(text = message, sender = Sender.USER)
        _chatMessages.value = _chatMessages.value + userMessage

        val loadingMessage = ChatMessage(text = "...", sender = Sender.MODEL)
        _chatMessages.value = _chatMessages.value + loadingMessage

        viewModelScope.launch {
            try {
                val request = ChatRequest(userId = userId, sessionId = sessionId, message = message)
                val response = chatApiService.sendMessage(request)

                // ▼▼▼▼▼ [핵심 수정] 더욱 강력해진 JSON 파싱 로직 ▼▼▼▼▼
                val rawResponse = response.chatResponse
                var displayText: String

                // 1. 응답 문자열에서 첫 '{' 와 마지막 '}'를 찾아 순수 JSON 부분만 추출합니다.
                val firstBrace = rawResponse.indexOf('{')
                val lastBrace = rawResponse.lastIndexOf('}')

                if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                    val jsonString = rawResponse.substring(firstBrace, lastBrace + 1)

                    try {
                        // 2. 추출한 순수 JSON 문자열을 파싱합니다.
                        val gson = Gson()
                        val analysisResponse = gson.fromJson(jsonString, AiAnalysisResponse::class.java)

                        // 3. 파싱 성공 시, 사용자용 텍스트를 가져옵니다. null이 아니면 사용하고, null이면 안전하게 원본 JSON을 보여줍니다.
                        displayText = analysisResponse.response_for_user ?: jsonString

                    } catch (e: Exception) {
                        // 4. JSON 추출 후 파싱에 실패하면(형식이 잘못된 경우), 원본 응답을 그대로 사용합니다.
                        Log.e("JsonParseError", "추출된 JSON 파싱 실패: ${e.message}")
                        displayText = rawResponse
                    }
                } else {
                    // 5. 문자열에서 '{' 또는 '}'를 찾지 못했다면, 일반 텍스트이므로 그대로 사용합니다.
                    displayText = rawResponse
                }
                // ▲▲▲▲▲ [핵심 수정] 여기까지 ▲▲▲▲▲

                // 최종적으로 정제된 텍스트로 메시지 객체를 생성합니다.
                val modelMessage = ChatMessage(text = displayText.trim(), sender = Sender.MODEL)

                // 대기 메시지를 실제 응답으로 교체합니다.
                _chatMessages.value = _chatMessages.value.dropLast(1) + modelMessage

            } catch (e: Exception) {
                Log.e("ChatViewModel", "메시지 전송 실패", e)
                val errorMessage = ChatMessage(text = "오류가 발생했습니다: ${e.message}", sender = Sender.MODEL)
                _chatMessages.value = _chatMessages.value.dropLast(1) + errorMessage
            }
        }
    }
}

data class State(
    val permissionRequested: Boolean,
    val permissionsGranted: Boolean,
    val steps: StepData,
    val refresh: Boolean,
    val errorLevel: HealthError?,
)