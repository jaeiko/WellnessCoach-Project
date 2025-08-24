// HealthDataSyncWorker.kt
package com.samsung.health.mysteps.background

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.samsung.health.mysteps.domain.ReadSleepDataUseCase
import com.samsung.health.mysteps.domain.ReadStepDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltWorker
class HealthDataSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    // ▼▼▼ [수정 1] 수면 데이터를 가져올 UseCase도 주입받습니다. ▼▼▼
    private val readStepDataUseCase: ReadStepDataUseCase,
    private val readSleepDataUseCase: ReadSleepDataUseCase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "HealthDataSyncWorker"
        private const val TAG = "HealthDataSyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "백그라운드 건강 데이터 동기화 작업을 시작합니다.")

        try {
            // ▼▼▼ [수정 2] '오늘' 날짜를 기준으로 데이터를 가져옵니다. ▼▼▼
            val today = LocalDate.now()
            Log.d(TAG, "데이터 기준 날짜: $today")

            // 1. 걸음 수 데이터를 읽어옵니다.
            val stepsData = readStepDataUseCase.invoke(today)
            Log.d(TAG, "걸음 수 데이터 읽기 성공: ${stepsData.count} 걸음")

            // 2. 수면 데이터를 읽어옵니다. (오늘 날짜 기준 -> 어젯밤 수면)
            val sleepData = readSleepDataUseCase.invoke(today)
            if (sleepData != null) {
                Log.d(TAG, "수면 데이터 읽기 성공: 총 ${sleepData.totalSleepMinutes}분")
            } else {
                Log.d(TAG, "수면 데이터가 없습니다.")
            }

            // 3. Firestore에 전송할 데이터를 구성합니다.
            val db = Firebase.firestore
            val userId = "user_1" // TODO: 추후 사용자 인증 시스템과 연동 필요
            val dateString = today.format(DateTimeFormatter.ISO_LOCAL_DATE) // "yyyy-MM-dd"

            // 걸음 수 데이터 구성
            val exerciseData = mapOf(
                "exercise_type" to "STEPS_DAILY",
                "stats" to mapOf(
                    "total_steps" to stepsData.count,
                    "goal" to stepsData.goal,
                    "hourly_steps" to stepsData.hourly.associate { it.startTime.hour.toString() to it.count }
                )
            )

            val healthLog = mutableMapOf<String, Any>(
                "exercise_data" to listOf(exerciseData),
                "timestamp" to FieldValue.serverTimestamp()
            )

            // ▼▼▼ [수정 3] 수면 데이터가 있을 경우, healthLog에 추가합니다. ▼▼▼
            sleepData?.let {
                val sleepPayload = mapOf(
                    "duration_minutes" to it.totalSleepMinutes,
                    "stages" to it.stages.map { stage ->
                        mapOf("stage" to stage.stage, "duration_minutes" to stage.durationMinutes)
                    }
                )
                healthLog["sleep_data"] = sleepPayload
            }
            // ▲▲▲

            // 4. Firestore에 데이터를 저장(병합)합니다.
            db.collection("users").document(userId).collection("health_logs").document(dateString)
                .set(healthLog, SetOptions.merge())
                .addOnSuccessListener { Log.d(TAG, "Firestore 백그라운드 데이터 전송 성공!") }
                .addOnFailureListener { e -> Log.w(TAG, "Firestore 백그라운드 데이터 전송 실패", e) }

            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "백그라운드 작업 중 오류 발생", e)
            return Result.failure()
        }
    }
}