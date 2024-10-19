package com.example.myapplication

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat

public class WeatherView @JvmOverloads constructor(
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

        // Set a minimum height to ensure the view is visible
        minimumHeight = 200 // Adjust this value as needed
    }

    fun updateWeatherInfo(temp: String, desc: String) {
        temperature.text = temp
        weatherDescription.text = desc
    }

    fun setWeatherIcon(iconResId: Int) {
        weatherIcon.setImageResource(iconResId)
    }
}