package com.example.forestgardeningapp

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>
)

data class Main(
    val temp: Double
)

data class Weather(
    val id: Int,
    val description: String
)