package com.example.forestgardeningapp

data class WeatherResults(
    val main: Main,
    val name: String
) {
    data class Main(
        val temp: Float
    )
}