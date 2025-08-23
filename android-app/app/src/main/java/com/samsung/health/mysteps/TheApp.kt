// TheApp.kt

package com.samsung.health.mysteps

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp
import com.samsung.health.mysteps.background.HealthDataSyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager


@HiltAndroidApp
class TheApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Hilt Worker를 위한 Configuration 제공
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        // 🔽 [핵심 추가] Firestore 설정을 변경하는 코드를 추가합니다.
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder(firestore.firestoreSettings)
            .setLocalCacheSettings(com.google.firebase.firestore.MemoryCacheSettings.newBuilder().build())
            .build()
        firestore.firestoreSettings = settings
        setupRecurringWork()
    }

    private fun setupRecurringWork() {
        // 1. 작업 요청 만들기: 1시간마다 HealthDataSyncWorker를 실행하도록 설정합니다.
        // (안드로이드 제약 상 최소 반복 주기는 15분입니다.)
        val repeatingRequest = PeriodicWorkRequestBuilder<HealthDataSyncWorker>(15, TimeUnit.MINUTES)
            .build()

        // 2. WorkManager에 작업 예약
        // ExistingPeriodicWorkPolicy.KEEP은 동일한 이름의 작업이 이미 예약되어 있다면, 기존 작업을 유지하고 새 요청을 무시합니다.
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            HealthDataSyncWorker.WORK_NAME, // 작업의 고유 이름
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
    }
}