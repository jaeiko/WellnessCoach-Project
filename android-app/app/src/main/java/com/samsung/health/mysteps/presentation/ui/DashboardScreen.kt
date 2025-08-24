// DashboardScreen.kt
package com.samsung.health.mysteps.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samsung.health.mysteps.data.model.Day
import com.samsung.health.mysteps.data.model.HeartRateData
import com.samsung.health.mysteps.data.model.SleepData
import com.samsung.health.mysteps.data.model.StepData
import java.time.LocalDate

@Composable
fun DashboardScreen(
    steps: StepData,
    sleepData: SleepData?,
    heartRateData: HeartRateData?,
    weekDays: List<Day>,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        // 상단에 날짜 선택 캘린더 UI를 추가합니다.
        WeekCalendar(days = weekDays, onDateSelected = onDateSelected)
        Spacer(modifier = Modifier.height(16.dp))

        // 건강 데이터 카드들을 보여주는 스크롤 리스트입니다.
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StepsFromTodayCard(stepCount = steps.count, stepGoal = steps.goal)
            }
            // TODO: 여기에 SleepSummaryCard, HeartRateCard 등을 추가하면 대시보드가 완성됩니다.
        }
    }
}