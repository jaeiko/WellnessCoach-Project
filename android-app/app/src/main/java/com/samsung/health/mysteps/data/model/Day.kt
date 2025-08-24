// Day.kt
package com.samsung.health.mysteps.data.model

import java.time.LocalDate

data class Day(
    val date: LocalDate,
    val isSelected: Boolean = false
)