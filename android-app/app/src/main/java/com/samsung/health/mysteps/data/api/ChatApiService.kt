package com.samsung.health.mysteps.data.api

import com.samsung.health.mysteps.data.model.ChatRequest
import com.samsung.health.mysteps.data.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApiService {
    @POST("chat")
    suspend fun sendMessage(@Body request: ChatRequest): ChatResponse
}