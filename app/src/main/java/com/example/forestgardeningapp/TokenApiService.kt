package com.example.forestgardeningapp

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TokenApiService {
    @POST("registerToken")
    suspend fun registerToken(@Body tokenRequest: TokenRequest): Response<Unit>
}

data class TokenRequest(val token: String)