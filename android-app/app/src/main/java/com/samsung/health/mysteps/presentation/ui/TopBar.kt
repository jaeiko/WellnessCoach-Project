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
package com.samsung.health.mysteps.presentation.ui

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samsung.health.mysteps.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onPermissionsInvoked: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    TopAppBar(
        actions = {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.menu_caption)
                )
            }
            DropdownMenu(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                expanded = expanded,
                onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    modifier = Modifier
                        .height(34.dp),
                    text = {
                        Text(
                            text = stringResource(R.string.menu_item_Connect),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    onClick = { expanded = false; onPermissionsInvoked() },
                )
            }
        },
        title = {
            Text(
                style = MaterialTheme.typography.titleMedium,
                text = stringResource(R.string.app_name),
                color = MaterialTheme.colorScheme.onPrimary
            )
        },
    )
}