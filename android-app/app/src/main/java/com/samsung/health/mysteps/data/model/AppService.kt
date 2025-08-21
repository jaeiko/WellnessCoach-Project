// android-app/app/src/main/java/com/samsung/health/mysteps/data/model/AppService.kt

package com.samsung.health.mysteps.data.model

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AppService {
    // HTTP POST 메서드로 "chat" 경로에 요청을 보냅니다.
    @POST("chat")
    suspend fun sendChatMessage(@Body request: ChatRequest): Response<ChatResponse>
}