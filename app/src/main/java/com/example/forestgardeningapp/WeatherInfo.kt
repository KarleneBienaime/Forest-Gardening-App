package com.example.forestgardeningapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherInfo : ViewModel() {
    private val _weatherData = MutableStateFlow<WeatherData?>(null)
    val weatherData: StateFlow<WeatherData?> = _weatherData.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    fun fetchWeather(cityName: String, apiKey: String) {
        viewModelScope.launch {
            try {
                val data = getWeatherData(cityName, apiKey)
                _weatherData.value = data
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
            }
        }
    }

    // Placeholder method for network call
    private fun getWeatherData(cityName: String, apiKey: String): WeatherData {

        val tempInCelsius = 25.0
        val tempInFahrenheit = tempInCelsius * 9 / 5 + 32
        return WeatherData("Test Location", MainData(tempInFahrenheit))
    }
}

data class WeatherData(val name: String, val main: MainData)
data class MainData(val temp: Double)
