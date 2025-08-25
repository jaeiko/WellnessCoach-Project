package com.samsung.health.mysteps.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.health.mysteps.data.model.ChatMessage
import com.samsung.health.mysteps.data.model.Sender
import com.samsung.health.mysteps.presentation.MainViewModel
import androidx.compose.ui.unit.sp // sp 유닛 import 확인
import java.text.SimpleDateFormat // 시간 포맷팅을 위한 import
import java.util.Date // Date 객체 사용을 위한 import
import java.util.Locale // Locale 설정을 위한 import

@Composable
fun ChatScreen(viewModel: MainViewModel = viewModel()) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    var userInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        // 메시지 목록
        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            reverseLayout = true // 최신 메시지가 하단에 오도록
        ) {
            items(chatMessages.reversed()) { message ->
                MessageBubble(message = message)
            }
        }

        // 메시지 입력 필드
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                placeholder = { Text("메시지를 입력하세요...") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions {
                    if (userInput.isNotBlank()) {
                        viewModel.sendMessage(userInput)
                        userInput = ""
                        keyboardController?.hide()
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (userInput.isNotBlank()) {
                    viewModel.sendMessage(userInput)
                    userInput = ""
                    keyboardController?.hide()
                }
            }) {
                Text("전송")
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    // 1. Long 타입의 timestamp를 "오후 2:57" 과 같은 형식의 문자열로 변환합니다.
    val timeFormatter = SimpleDateFormat("a h:mm", Locale.KOREAN)
    val displayTime = timeFormatter.format(Date(message.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (message.sender == Sender.USER) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom // 말풍선과 시간을 하단에 정렬합니다.
    ) {
        // 메시지 주체에 따라 말풍선과 시간의 순서를 결정합니다.
        if (message.sender == Sender.USER) {
            // 사용자: 시간 -> 말풍선 순서
            Text(
                text = displayTime,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp, // 글자 크기를 약간 작게 설정
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 말풍선 모양 결정 (기존 코드와 동일)
        val bubbleShape = if (message.sender == Sender.USER) {
            RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 4.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            )
        }

        // 말풍선 카드 (기존 코드와 동일)
        Card(
            shape = bubbleShape,
            colors = CardDefaults.cardColors(
                containerColor = if (message.sender == Sender.USER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 메시지 주체에 따라 말풍선과 시간의 순서를 결정합니다.
        if (message.sender == Sender.MODEL) {
            // AI: 말풍선 -> 시간 순서
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayTime,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}