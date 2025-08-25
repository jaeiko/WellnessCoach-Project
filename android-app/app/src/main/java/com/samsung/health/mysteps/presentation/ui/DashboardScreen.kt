// android-app/app/src/main/java/com/samsung/health/mysteps/presentation/ui/DashboardScreen.kt

package com.samsung.health.mysteps.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.health.mysteps.data.model.Day
import com.samsung.health.mysteps.data.model.HeartRateData
import com.samsung.health.mysteps.data.model.SleepData
import com.samsung.health.mysteps.data.model.StepData
import java.time.LocalDate


// AppTitle 컴포저블 (이전 단계에서 추가)
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
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppTitle()
        Spacer(modifier = Modifier.height(16.dp))
        WeekCalendar(days = weekDays, onDateSelected = onDateSelected)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 걸음 수 카드
            item {
                StepsFromTodayCard(stepCount = steps.count, stepGoal = steps.goal)
            }

            // ▼▼▼ [핵심 수정 1] 수면 데이터 처리 로직 변경 ▼▼▼
            item {
                if (sleepData != null) {
                    // 데이터가 있으면 기존 카드 표시
                    SleepSummaryCard(sleepData = sleepData)
                } else {
                    // 데이터가 없으면 새로 만든 NoDataCard 표시
                    NoDataCard(
                        icon = Icons.Default.Bedtime,
                        title = "수면 데이터 없음",
                        message = "데이터가 기록되면 여기에 표시됩니다."
                    )
                }
            }
            // ▲▲▲ 수정 완료 ▲▲▲

            // ▼▼▼ [핵심 추가 2] 심박수 및 기타 데이터 카드 영역 추가 ▼▼▼
            item {
                if (heartRateData != null) {
                    // TODO: 추후 심박수 데이터를 표시할 HeartRateSummaryCard() 구현
                } else {
                    // 데이터가 없으면 NoDataCard 표시 (데모용)
                    NoDataCard(
                        icon = Icons.Default.Favorite,
                        title = "심박수 데이터 없음",
                        message = "곧 연동될 기능입니다."
                    )
                }
            }
            // ▲▲▲ 추가 완료 ▲▲▲
        }
    }
}