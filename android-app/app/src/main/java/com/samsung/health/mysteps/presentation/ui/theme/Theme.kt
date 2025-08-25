/*
 * Copyright 2025 Samsung Electronics Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samsung.health.mysteps.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = VibrantCoral,
    primaryContainer = VibrantCoral.copy(alpha = 0.2f), // 메인 색의 투명한 버전
    onPrimary = Color.White,

    background = PureWhite,
    onBackground = DeepBlue,

    surface = PureWhite,
    surfaceVariant = LightGrey,
    onSurface = DeepBlue,
    onSurfaceVariant = MediumGrey
)
private val DarkColorScheme = darkColorScheme(

    primary = AppleGreen,
    onPrimary = AlmostWhite,
    primaryContainer = DarkGrey21,

    onSecondary = Grey97,
    secondaryContainer = DarkGrey2d,

    tertiaryContainer = MiddleGrey3a,

    onBackground = Color.Black,
    background = Color.Black,

    surface = Color.Black,
    surfaceVariant = DarkGrey17
)

@Composable
fun PhoneAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

