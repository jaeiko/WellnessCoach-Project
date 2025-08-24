// WeekCalendar.kt
package com.samsung.health.mysteps.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samsung.health.mysteps.data.model.Day
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeekCalendar(
    days: List<Day>,
    onDateSelected: (LocalDate) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(days) { day ->
            DayItem(day = day, onDateSelected = onDateSelected)
        }
    }
}

@Composable
fun DayItem(
    day: Day,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onDateSelected(day.date) }
            .background(
                if (day.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
            style = MaterialTheme.typography.bodySmall,
            color = if (day.isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (day.isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}