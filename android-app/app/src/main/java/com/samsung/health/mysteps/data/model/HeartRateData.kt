package com.samsung.health.mysteps.data.model

data class HeartRateData(
    val lastMeasuredTime: String,
    val lastHeartRate: Int,
    val dailyMin: Int,
    val dailyMax: Int,
)