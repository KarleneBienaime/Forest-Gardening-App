package com.example.forestgardeningapp

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat

class WeatherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val weatherIcon: ImageView
    private val temperature: TextView
    private val weatherDescription: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.weather_frame, this, true)

        weatherIcon = findViewById(R.id.weather_icon)
        temperature = findViewById(R.id.temperature)
        weatherDescription = findViewById(R.id.weather_description)

        // Set a background color to make the view visible
        setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
    }

    fun updateWeatherInfo(temp: String, description: String, iconResource: Int) {
        temperature.text = "$temp°C"
        weatherDescription.text = description
        weatherIcon.setImageResource(iconResource)
    }
}