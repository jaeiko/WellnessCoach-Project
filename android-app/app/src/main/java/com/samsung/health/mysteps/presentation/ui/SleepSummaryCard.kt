// /android-app/app/src/main/java/com/samsung/health/mysteps/presentation/ui/SleepSummaryCard.kt

package com.samsung.health.mysteps.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.health.mysteps.data.model.SleepData

// 수면 데이터를 예쁘게 표시해 줄 카드 UI 함수입니다.
@Composable
fun SleepSummaryCard(sleepData: SleepData) {
    // 총 수면 시간(분)을 시간과 분으로 변환합니다.
    val hours = sleepData.totalSleepMinutes / 60
    val minutes = sleepData.totalSleepMinutes % 60

    Card(
        shape = MaterialTheme.shapes.large, // 기존 카드들과 통일감을 줍니다.
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp) // 카드 내부에 여백을 줍니다.
        ) {
            Text(
                text = "🌙 수면 분석",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 총 수면 시간을 강조해서 보여줍니다.
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold)) {
                        append("$hours")
                    }
                    withStyle(style = SpanStyle(fontSize = 20.sp)) {
                        append("시간 ")
                    }
                    withStyle(style = SpanStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold)) {
                        append("$minutes")
                    }
                    withStyle(style = SpanStyle(fontSize = 20.sp)) {
                        append("분")
                    }
                },
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 각 수면 단계별 정보를 간단하게 보여줍니다.
            sleepData.stages.forEach { stage ->
                Text(
                    text = "${translateSleepStage(stage.stage)}: ${stage.durationMinutes}분",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 수면 단계 영문 이름을 한글로 변환해주는 도우미 함수입니다.
private fun translateSleepStage(stage: String): String {
    return when (stage) {
        "AWAKE" -> "깬 시간"
        "LIGHT" -> "얕은 수면"
        "DEEP" -> "깊은 수면"
        "REM" -> "렘 수면"
        else -> "알 수 없음"
    }
}