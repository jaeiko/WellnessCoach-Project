// DashboardScreen.kt
package com.samsung.health.mysteps.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samsung.health.mysteps.data.model.Day
import com.samsung.health.mysteps.data.model.HeartRateData
import com.samsung.health.mysteps.data.model.SleepData
import com.samsung.health.mysteps.presentation.ui.SleepSummaryCard
import com.samsung.health.mysteps.data.model.StepData
import java.time.LocalDate

@Composable
fun AppTitle() {
    Text(
        text = "WellnessCoachAI",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun DashboardScreen(
    steps: StepData,
    sleepData: SleepData?,
    heartRateData: HeartRateData?,
    weekDays: List<Day>,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        AppTitle()
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
            sleepData?.let {
                item {
                    SleepSummaryCard(sleepData = it)
                }
            }
            // TODO: 여기에 SleepSummaryCard, HeartRateCard 등을 추가하면 대시보드가 완성됩니다.
        }
    }
}