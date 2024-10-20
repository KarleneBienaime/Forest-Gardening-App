package com.example.forestgardeningapp

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("register_token")
    suspend fun registerToken(@Body tokenRequest: TokenRequest): Response<Unit>
}