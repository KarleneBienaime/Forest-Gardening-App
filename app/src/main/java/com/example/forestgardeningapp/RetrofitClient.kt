package com.example.forestgardeningapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val WEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/"
    private const val TOKEN_BASE_URL = "https://us-central1-com.example.forestgardeningapp.cloudfunctions.net/" // Replace with your actual server URL

    private val weatherRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(WEATHER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val tokenRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(TOKEN_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val weatherApiService: WeatherApiService by lazy {
        weatherRetrofit.create(WeatherApiService::class.java)
    }

    val tokenApiService: TokenApiService by lazy {
        tokenRetrofit.create(TokenApiService::class.java)
    }
}