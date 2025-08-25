// MainViewModel.kt
package com.samsung.health.mysteps.presentation


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.samsung.android.sdk.health.data.error.AuthorizationException
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.InvalidRequestException
import com.samsung.android.sdk.health.data.error.PlatformInternalException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import com.samsung.health.mysteps.data.api.ChatApiService
import com.samsung.health.mysteps.data.model.AiAnalysisResponse
import com.samsung.health.mysteps.data.model.ChatMessage
import com.samsung.health.mysteps.data.model.ChatRequest
import com.samsung.health.mysteps.data.model.Day
import com.samsung.health.mysteps.data.model.HealthError
import com.samsung.health.mysteps.data.model.HeartRateData
import com.samsung.health.mysteps.data.model.Sender
import com.samsung.health.mysteps.data.model.SleepData
import com.samsung.health.mysteps.data.model.StepData
import com.samsung.health.mysteps.domain.ArePermissionsGrantedUseCase
import com.samsung.health.mysteps.domain.ReadSleepDataUseCase
import com.samsung.health.mysteps.domain.ReadStepDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

private const val TAG = "MainViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(
    private val arePermissionsGrantedUseCase: ArePermissionsGrantedUseCase,
    private val readStepDataUseCase: ReadStepDataUseCase,
    private val readSleepDataUseCase: ReadSleepDataUseCase,
    // private val readHeartRateUseCase: ReadHeartRateUseCase, // 심박수 기능 추가 시 주석 해제
    private val chatApiService: ChatApiService
) : ViewModel() {

    private val sessionId = "session_${UUID.randomUUID()}"
    private val userId = "user_1" // TODO: 추후 사용자 인증 시스템과 연동 필요

    // 채팅 메시지 목록 (UI가 관찰)
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = """
                    안녕하세요! 저는 당신의 건강한 삶을 돕는 AI 웰니스 코치입니다. 😊

                    걸음 수, 수면 데이터 등을 분석하여 건강 상태를 알려드리고, 맞춤형 건강 루틴을 제안해 드릴 수 있어요.

                    먼저, 저에게 이렇게 물어보시는 건 어떨까요?
                    "오늘 내 건강 데이터 분석해줘"
                """.trimIndent(),
                sender = Sender.MODEL
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    // 날짜 선택 UI를 위한 상태들
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val weekDays = MutableStateFlow(createWeekDays(LocalDate.now()))

    // 채팅창 표시 여부 상태
    private val _isChatVisible = MutableStateFlow(false)
    val isChatVisible: StateFlow<Boolean> = _isChatVisible.asStateFlow()

    init {
        Log.i(TAG, "init()")
        checkPermissions()
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        weekDays.value = createWeekDays(date)
        readAllHealthData()
    }

    private fun createWeekDays(centerDate: LocalDate): List<Day> {
        return (-3..3).map {
            val date = centerDate.plusDays(it.toLong())
            Day(date, isSelected = date == centerDate)
        }
    }

    private fun checkPermissions() {
        viewModelScope.launch {
            try {
                val permissionsGranted = arePermissionsGrantedUseCase()
                _state.update { it.copy(permissionsGranted = permissionsGranted) }
                if (permissionsGranted) {
                    readAllHealthData()
                }
            } catch (e: HealthDataException) {
                handleHealthDataException(e)
            }
        }
    }

    fun readAllHealthData() {
        viewModelScope.launch {
            val dateToFetch = _selectedDate.value
            try {
                val steps = readStepDataUseCase.invoke(dateToFetch) // .invoke()를 명시적으로 호출
                val sleep = readSleepDataUseCase.invoke(dateToFetch) // .invoke()를 명시적으로 호출

                _state.update {
                    it.copy(
                        steps = steps,
                        sleepData = sleep,
                        heartRateData = null, // TODO: 나중에 심박수 기능 구현
                        errorLevel = null
                    )
                }
            } catch (e: HealthDataException) {
                handleHealthDataException(e)
            }
        }
    }

    fun sendMessage(message: String) {
        val userMessage = ChatMessage(text = message, sender = Sender.USER)
        _chatMessages.value = _chatMessages.value + userMessage

        val loadingMessage = ChatMessage(text = "...", sender = Sender.MODEL)
        _chatMessages.value = _chatMessages.value + loadingMessage

        viewModelScope.launch {
            try {
                val healthDataPayload = if (message.contains("분석")) {
                    // "오늘" 키워드가 있는지 확인합니다.
                    if (message.contains("오늘")) {
                        // "오늘"이 있으면, 현재 날짜 기준으로 데이터를 즉시 다시 가져옵니다.
                        Log.d(TAG, "오늘 데이터 분석 요청. 현재 날짜 데이터로 페이로드를 준비합니다.")
                        prepareHealthDataPayload(LocalDate.now())
                    } else {
                        // "오늘"이 없으면, 기존처럼 현재 state (선택된 날짜) 기준으로 데이터를 준비합니다.
                        Log.d(TAG, "선택된 날짜(${_selectedDate.value}) 데이터 분석 요청.")
                        prepareHealthDataPayload()
                    }
                } else {
                    null
                }

                val request = ChatRequest(
                    userId = userId,
                    sessionId = sessionId,
                    message = message,
                    healthData = healthDataPayload
                )

                val response = chatApiService.sendMessage(request)

                val rawResponse = response.chatResponse
                var displayText: String
                val firstBrace = rawResponse.indexOf('{')
                val lastBrace = rawResponse.lastIndexOf('}')
                if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                    val jsonString = rawResponse.substring(firstBrace, lastBrace + 1)
                    try {
                        val gson = Gson()
                        val analysisResponse = gson.fromJson(jsonString, AiAnalysisResponse::class.java)
                        displayText = analysisResponse.response_for_user ?: jsonString
                    } catch (e: Exception) {
                        Log.e("JsonParseError", "추출된 JSON 파싱 실패: ${e.message}")
                        displayText = rawResponse
                    }
                } else {
                    displayText = rawResponse
                }

                val modelMessage = ChatMessage(text = displayText.trim(), sender = Sender.MODEL)
                _chatMessages.value = _chatMessages.value.dropLast(1) + modelMessage

            } catch (e: Exception) {
                Log.e("ChatViewModel", "메시지 전송 실패", e)
                val errorMessage = ChatMessage(text = "오류가 발생했습니다: ${e.message}", sender = Sender.MODEL)
                _chatMessages.value = _chatMessages.value.dropLast(1) + errorMessage
            }
        }
    }

    private suspend fun prepareHealthDataPayload(date: LocalDate? = null): Map<String, Any> {
        val payload = mutableMapOf<String, Any>()

        val finalStepsData: StepData
        val finalSleepData: SleepData?

        if (date != null) {
            // 날짜가 지정된 경우, 해당 날짜의 데이터를 새로 읽어옵니다.
            finalStepsData = readStepDataUseCase(date)
            finalSleepData = readSleepDataUseCase(date)
        } else {
            // 날짜가 지정되지 않은 경우, 기존 state의 데이터를 사용합니다.
            finalStepsData = state.value.steps
            finalSleepData = state.value.sleepData
        }

        val exerciseData = mapOf(
            "exercise_type" to "STEPS_DAILY",
            "stats" to mapOf(
                "total_steps" to finalStepsData.count,
                "goal" to finalStepsData.goal,
                "hourly_steps" to finalStepsData.hourly.associate { it.startTime.hour.toString() to it.count }
            )
        )
        payload["exercise_data"] = listOf(exerciseData)

        finalSleepData?.let { sleepData ->
            val sleepPayload = mapOf(
                "duration_minutes" to sleepData.totalSleepMinutes,
                "stages" to sleepData.stages.map { mapOf("stage" to it.stage, "duration_minutes" to it.durationMinutes) }
            )
            payload["sleep_data"] = sleepPayload
        }

        Log.d(TAG, "백엔드로 전송할 데이터 (${date ?: _selectedDate.value}): $payload")
        return payload
    }

    fun showChat() {
        _isChatVisible.value = true
    }

    fun hideChat() {
        _isChatVisible.value = false
    }

    fun userAcceptedPermissions(agreed: Boolean) {
        _state.update { it.copy(permissionsGranted = agreed) }
        if (agreed) {
            readAllHealthData()
        }
    }

    fun handleHealthDataException(healthDataException: HealthDataException) {
        // ... (기존 코드와 동일)
    }
}

// ▼▼▼ [수정] State 데이터 클래스의 refresh 필드를 제거합니다. ▼▼▼
data class State(
    val permissionRequested: Boolean = false,
    val permissionsGranted: Boolean = false,
    val steps: StepData = StepData(0, 6000, arrayListOf()),
    val sleepData: SleepData? = null,
    val heartRateData: HeartRateData? = null,
    val errorLevel: HealthError? = null,
)