package com.example.forestgardeningapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.gridlayout.widget.GridLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var gardenGrid: GridLayout
    private var plantIconMap: MutableMap<String, Int> = HashMap()
    private var iconsLoaded = false
    private lateinit var weatherView: WeatherView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val apiKey = "125ff6d34010926bd081490ad208b1ad"

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupToolbar()
        setupBottomNavigation()
        setupFab()
        setupGardenGrid()
        loadIcons()
        weatherView = findViewById(R.id.weather_view)
        requestLocationPermission()

    }private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getLocationAndFetchWeather()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getLocationAndFetchWeather()
                } else {
                    Toast.makeText(this, "Location permission denied. Using default location.", Toast.LENGTH_LONG).show()
                    fetchWeatherData(40.7128, -74.0060) // Default to New York City
                }
                return
            }
        }
    }

    private fun getLocationAndFetchWeather() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                location?.let {
                    fetchWeatherData(it.latitude, it.longitude)
                } ?: run {
                    Toast.makeText(this, "Location not available. Using default location.", Toast.LENGTH_LONG).show()
                    fetchWeatherData(40.7128, -74.0060) // Default to New York City
                }
            }
    }

    private fun fetchWeatherData(lat: Double, lon: Double) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.weatherApiService.getWeatherByCoordinates(lat, lon, apiKey)
                updateWeatherUI(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching weather data", e)
                Toast.makeText(this@MainActivity, "Failed to fetch weather data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun setupBottomNavigation() {
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_dashboard -> true
                R.id.navigation_notifications -> true
                else -> false
            }
        }
    }

    private fun setupFab() {
        val fabAddPlant: FloatingActionButton = findViewById(R.id.fab_add_plant)
        fabAddPlant.setOnClickListener { anchorView ->
            showPlantSelectionMenu(anchorView)
        }
    }

    private fun setupGardenGrid() {
        gardenGrid = findViewById<GridLayout>(R.id.garden_grid)
    }

    private fun loadIcons() {
        lifecycleScope.launch {
            try {
                val iconMap = withContext(Dispatchers.IO) {
                    // Simulate icon loading process
                    // In a real app, you'd load icons from resources or a remote source
                    mapOf(
                        "tomato" to R.drawable.ic_plant,
                        "lettuce" to R.drawable.ic_plant,
                        "carrot" to R.drawable.ic_plant
                        // Add more plant icons as needed
                    )
                }
                onIconsLoaded(iconMap)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading icons", e)
                Toast.makeText(this@MainActivity, "Failed to load plant icons", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateWeatherUI(response: WeatherResponse) {
        val temperature = response.main.temp.toString()
        val weatherDescription = response.weather[0].description
        weatherView.updateWeatherInfo(temperature, weatherDescription)
    }

    private fun onIconsLoaded(iconMap: Map<String, Int>) {
        plantIconMap.clear()
        plantIconMap.putAll(iconMap)
        iconsLoaded = true
        Log.d(TAG, "Plant icons loaded in MainActivity: ${plantIconMap.size}")
    }

    private fun showPlantSelectionMenu(anchorView: View) {
        if (!iconsLoaded) {
            Toast.makeText(this, "Plant icons are still loading. Please wait.", Toast.LENGTH_SHORT).show()
            return
        }

        PopupMenu(this, anchorView).apply {
            menuInflater.inflate(R.menu.plant_options_menu, menu)
            setOnMenuItemClickListener { item: MenuItem ->
                val plantName = item.title.toString()
                Log.d(TAG, "Selected plant: $plantName")
                addPlantToNextAvailableCell(plantName)
                true
            }
            show()
        }
    }

    private fun addPlantToNextAvailableCell(plantName: String) {
        for (i in 0 until gardenGrid.childCount) {
            val cell = gardenGrid.getChildAt(i) as? FrameLayout
            if (cell?.childCount == 0) {
                addPlantToCell(cell, plantName)
                return
            }
        }
        Toast.makeText(this, "No empty cells available", Toast.LENGTH_SHORT).show()
    }

    private fun addPlantToCell(cell: FrameLayout, plantName: String) {
        lifecycleScope.launch {
            val plantIcon = createPlantIcon(plantName)
            withContext(Dispatchers.Main) {
                cell.removeAllViews()
                cell.addView(plantIcon)
                setupPlantRemoval(cell, plantIcon)
            }
        }
    }

    private suspend fun createPlantIcon(plantName: String): ImageView =
        withContext(Dispatchers.Default) {
            ImageView(this@MainActivity).apply {
                setImageResource(getPlantIconResource(plantName))
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        }

    private fun getPlantIconResource(plantName: String): Int {
        val normalizedName = plantName.lowercase(Locale.getDefault()).replace(" ", "_")
        return plantIconMap[normalizedName] ?: R.drawable.ic_plant
    }

    private fun setupPlantRemoval(cell: FrameLayout, plantIcon: ImageView) {
        plantIcon.setOnLongClickListener {
            showRemovePlantDialog(cell)
            true
        }
    }

    private fun showRemovePlantDialog(cell: FrameLayout) {
        PopupMenu(this, cell).apply {
            menu.add("Remove Plant")
            setOnMenuItemClickListener { item: MenuItem ->
                when (item.title) {
                    "Remove Plant" -> {
                        removePlantFromCell(cell)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun removePlantFromCell(cell: FrameLayout) {
        cell.removeAllViews()
        Toast.makeText(this, "Plant removed", Toast.LENGTH_SHORT).show()
    }



}