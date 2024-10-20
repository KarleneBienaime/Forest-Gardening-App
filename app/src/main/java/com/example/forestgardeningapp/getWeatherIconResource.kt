package com.example.forestgardeningapp

fun getWeatherIconResource(weatherId: Int): Int {
    return when (weatherId) {
        in 500..531 -> R.drawable.rainy
        800 -> R.drawable.sun
        in 801..804 -> R.drawable.cloudy
        else -> android.R.drawable.ic_menu_help
    }
}