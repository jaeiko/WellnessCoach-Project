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
import com.samsung.health.mysteps.domain.ReadStepDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class HealthDataSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    // Hilt를 통해 UseCase를 주입받습니다.
    private val readStepDataUseCase: ReadStepDataUseCase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "HealthDataSyncWorker"
        private const val TAG = "HealthDataSyncWorker"
    }

    // 백그라운드에서 실행될 실제 작업 내용입니다.
    override suspend fun doWork(): Result {
        Log.d(TAG, "백그라운드 걸음 수 데이터 동기화 작업을 시작합니다.")

        try {
            // 1. 걸음 수 데이터를 읽어옵니다. (기존 로직 재사용)
            val stepsData = readStepDataUseCase()
            Log.d(TAG, "오늘의 걸음 수 데이터 읽기 성공: ${stepsData.count} 걸음")

            // 2. Firestore에 전송합니다. (ViewModel의 로직을 이곳으로 가져옴)
            val db = Firebase.firestore
            val userId = "user_1" // TODO: 추후 사용자 인증 시스템과 연동 필요
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val totalSteps = stepsData.count
            val stepGoal = stepsData.goal
            val hourlySteps = stepsData.hourly.associate { it.startTime.hour.toString() to it.count }

            val exerciseData = hashMapOf(
                "exercise_type" to "STEPS_DAILY",
                "end_time" to com.google.firebase.Timestamp.now(),
                "stats" to hashMapOf(
                    "total_steps" to totalSteps,
                    "goal" to stepGoal,
                    "hourly_steps" to hourlySteps
                )
            )

            val healthLog = hashMapOf(
                "exercise_data" to listOf(exerciseData),
                "timestamp" to FieldValue.serverTimestamp()
            )

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