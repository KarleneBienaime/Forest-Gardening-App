package com.example.forestgardeningapp


import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.gridlayout.widget.GridLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var gardenGrid: GridLayout
    private var plantIconMap: MutableMap<String, Int> = HashMap()
    private var iconsLoaded = false
    private lateinit var weatherView: WeatherView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 2
    private val apiKey = "125ff6d34010926bd081490ad208b1ad"
    private val ADD_PLANT_BUTTON_ID = 1001

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()
        requestNotificationPermission()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupToolbar()
        setupBottomNavigation()
        setupGardenGrid()
        loadIcons()

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM Token", token)
            }
        }

        weatherView = findViewById(R.id.weather_view)
        requestLocationPermission()
        updateAllCellVisuals()
    }

    private fun showNotification(title: String, content: String) {
        val builder = NotificationCompat.Builder(this, "FOREST_GARDENING_CHANNEL")
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                showNotificationWithPermission(builder)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            showNotificationWithPermission(builder)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("FOREST_GARDENING_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotificationWithPermission(builder: NotificationCompat.Builder) {
        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when showing notification: ${e.message}")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun checkCompanionPlanting(selectedPlant: String) {
        Log.d(TAG, "Checking companion planting for: $selectedPlant")
        val existingPlants = getExistingPlants().map { it.lowercase(Locale.ROOT) }
        val selectedPlantLower = selectedPlant.lowercase(Locale.ROOT)
        val companions = CompanionPlantingData.companionPlantingMap[selectedPlantLower] ?: emptyList()
        val incompatibles = CompanionPlantingData.incompatiblePlantingMap[selectedPlantLower] ?: emptyList()

        val hasCompanions = existingPlants.any { it in companions }
        val hasIncompatibles = existingPlants.any { it in incompatibles }

        when {
            hasIncompatibles -> {
                Log.d(TAG, "Found incompatible plants")
                showIncompatiblePlantWarning(selectedPlant, existingPlants.filter { it in incompatibles }.toSet())
            }
            hasCompanions -> {
                Log.d(TAG, "Found companion plants")
                showCompanionPlantMessage(selectedPlant, existingPlants.filter { it in companions }.toSet())
            }
            else -> {
                Log.d(TAG, "No companions or incompatibles found")
            }
        }

        // Always add the plant
        Log.d(TAG, "Calling addPlantToNextAvailableCell")
        addPlantToNextAvailableCell(selectedPlant)
    }

    private fun getExistingPlants(): List<String> {
        return (0 until gardenGrid.childCount - 1).mapNotNull { i ->
            val cell = gardenGrid.getChildAt(i) as? FrameLayout
            val plantImage = cell?.findViewById<ImageView>(R.id.plant_image)
            plantImage?.tag as? String
        }
    }

    private fun showIncompatiblePlantWarning(selectedPlant: String, incompatibles: Set<String>) {
        val message = "Warning: $selectedPlant is not compatible with ${incompatibles.joinToString(", ")}. " +
                "Do you still want to plant it?"
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Incompatible Planting")
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> addPlantToNextAvailableCell(selectedPlant) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showCompanionPlantMessage(selectedPlant: String, companions: Set<String>) {
        val message = "Great choice! $selectedPlant is a companion plant for ${companions.joinToString(", ")}."
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun requestLocationPermission() {
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

    private fun setupGardenGrid() {
        gardenGrid = findViewById(R.id.garden_grid)

        val rows = 4
        val cols = 5
        val totalCells = rows * cols - 1  // Subtract 1 to leave space for the "Add Plant" button

        for (i in 0 until totalCells) {
            val cellView = layoutInflater.inflate(R.layout.grid_cell_layout, gardenGrid, false) as FrameLayout
            cellView.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = resources.getDimensionPixelSize(R.dimen.cell_height)
                setMargins(2, 2, 2, 2)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            gardenGrid.addView(cellView)
        }

        val addPlantButton = ImageButton(this).apply {
            id = ADD_PLANT_BUTTON_ID
            setImageResource(R.drawable.ic_add_box)
            setBackgroundResource(R.drawable.plot_background4)
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.DBrown))
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = resources.getDimensionPixelSize(R.dimen.cell_height)
                setMargins(2, 2, 2, 2)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            setOnClickListener { showPlantSelectionMenu(it) }
        }
        gardenGrid.addView(addPlantButton)
        updateAllCellVisuals()
    }

    private fun loadIcons() {
        lifecycleScope.launch {
            try {
                val iconMap = withContext(Dispatchers.IO) {
                    // Simulate icon loading process
                    // In a real app, you'd load icons from resources or a remote source
                    mapOf(
                        "tomatoes" to R.drawable.seed,
                        "bell pepper" to R.drawable.seed,
                        "basil" to R.drawable.seed,
                        // Add more plant icons as needed
                    )
                }
                onIconsLoaded(iconMap)
                // Remove this line
                // updateAllCellVisuals()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading icons", e)
                Toast.makeText(this@MainActivity, "Failed to load plant icons", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateWeatherUI(response: WeatherResponse) {
        val temperatureCelsius = response.main.temp
        val temperatureFahrenheit = celsiusToFahrenheit(temperatureCelsius)
        val weatherDescription = response.weather.firstOrNull()?.description ?: "Unknown"
        val weatherIconId = response.weather.firstOrNull()?.id ?: 0
        val iconResource = getWeatherIconResource(weatherIconId)
        weatherView.updateWeatherInfo("${temperatureFahrenheit}°F", weatherDescription, iconResource)
    }

    private fun celsiusToFahrenheit(celsius: Double): Int {
        return ((celsius * 9/5) + 32).roundToInt()
    }

    private fun onIconsLoaded(iconMap: Map<String, Int>) {
        plantIconMap.clear()
        plantIconMap.putAll(iconMap)
        iconsLoaded = true
        Log.d(TAG, "Plant icons loaded in MainActivity: ${plantIconMap.size}")
    }

    private fun showPlantSelectionMenu(anchorView: View) {
        Log.d(TAG, "Showing plant selection menu")
        if (!iconsLoaded) {
            Log.d(TAG, "Plant icons are still loading")
            Toast.makeText(this, "Plant icons are still loading. Please wait.", Toast.LENGTH_SHORT).show()
            return
        }

        PopupMenu(this, anchorView).apply {
            menuInflater.inflate(R.menu.plant_options_menu, menu)
            setOnMenuItemClickListener { item: MenuItem ->
                val plantName = item.title.toString()
                Log.d(TAG, "Selected plant from menu: $plantName")
                checkCompanionPlanting(plantName)
                true
            }
            show()
        }
    }

    private fun addPlantToNextAvailableCell(plantName: String) {
        for (i in 0 until gardenGrid.childCount - 1) { // Exclude the last cell (Add Plant button)
            val cell = gardenGrid.getChildAt(i) as? FrameLayout
            val plantImage = cell?.findViewById<ImageView>(R.id.plant_image)
            if (cell != null && plantImage?.drawable == null) {
                addPlantToCell(cell, plantName)
                Log.d(TAG, "Plant added to cell $i: $plantName")
                checkAndShowPlantRelationships(plantName)  // Call the function here
                return
            }
        }
        Toast.makeText(this, "No empty cells available", Toast.LENGTH_SHORT).show()
    }

    private fun checkAndShowPlantRelationships(plantName: String) {
        Log.d(TAG, "checkAndShowPlantRelationships called with plant: $plantName")
        val existingPlants = getExistingPlants()
        updatePlantRelationshipVisuals(existingPlants)
        showPlantRelationshipInfo(plantName, existingPlants)
    }

    private fun addPlantToCell(cell: FrameLayout, plantName: String) {
        val plantImage = cell.findViewById<ImageView>(R.id.plant_image)
        plantImage.setImageResource(getPlantIconResource(plantName))
        plantImage.tag = plantName.lowercase(Locale.ROOT)
        cell.findViewById<View>(R.id.cell_overlay).setBackgroundColor(Color.TRANSPARENT)
        setupPlantRemoval(cell, plantImage)

        // Schedule a delayed notification
        CoroutineScope(Dispatchers.Default).launch {
            delay(30000) // Wait for 30 seconds
            withContext(Dispatchers.Main) {
                showNotification(
                    "Water me, please!",
                    "Hi, I'm your new $plantName! I'm thirsty, can you water me?"
                )
            }
        }

        // Update plant relationships
        val existingPlants = getExistingPlants()
        updatePlantRelationshipVisuals(existingPlants)
        showPlantRelationshipInfo(plantName, existingPlants)
    }

    private fun updatePlantRelationshipVisuals(plants: List<String>) {
        Log.d(TAG, "Updating visuals for plants: $plants")
        for (i in 0 until gardenGrid.childCount - 1) { // Exclude the last cell (Add Plant button)
            val cell = gardenGrid.getChildAt(i) as? FrameLayout ?: continue
            val plantImage = cell.findViewById<ImageView>(R.id.plant_image)
            val cellPlantName = plantImage.tag as? String
            val cellBorder = cell.findViewById<View>(R.id.cell_border)

            if (cellBorder == null) {
                Log.e(TAG, "Cell border view not found for cell $i")
                continue
            }

            Log.d(TAG, "Checking cell $i with plant: $cellPlantName")

            if (cellPlantName != null) {
                val companions = CompanionPlantingData.companionPlantingMap[cellPlantName.lowercase(
                    Locale.getDefault()
                )] ?: emptyList()
                val incompatibles = CompanionPlantingData.incompatiblePlantingMap[cellPlantName.lowercase(
                    Locale.getDefault()
                )] ?: emptyList()
                Log.d(TAG, "Cell $i: Companions=$companions, Incompatibles=$incompatibles")

                val borderColor = when {
                    plants.any { it.lowercase(Locale.getDefault()) in incompatibles } -> {
                        Log.d(TAG, "Cell $i: Incompatible plant detected")
                        ContextCompat.getColor(this, R.color.incompatible_plant_border)
                    }
                    plants.any { it.lowercase(Locale.getDefault()) in companions } -> {
                        Log.d(TAG, "Cell $i: Companion plant detected")
                        ContextCompat.getColor(this, R.color.companion_plant_border)
                    }
                    else -> {
                        Log.d(TAG, "Cell $i: No special relationship detected")
                        Color.TRANSPARENT
                    }
                }

                // Apply the border color
                (cellBorder.background as GradientDrawable).setStroke(5, borderColor)
                Log.d(TAG, "Applying border color to cell $i: Color=${String.format("#%08X", borderColor)}")
            } else {
                Log.d(TAG, "Cell $i is empty, applying transparent border.")
                (cellBorder.background as GradientDrawable).setStroke(5, Color.TRANSPARENT)
            }
        }
    }

    private fun showPlantRelationshipInfo(plantName: String, existingPlants: List<String>) {
        val companions = CompanionPlantingData.companionPlantingMap[plantName] ?: emptyList()
        val incompatibles = CompanionPlantingData.incompatiblePlantingMap[plantName] ?: emptyList()

        val presentCompanions = existingPlants.filter { it in companions }
        val presentIncompatibles = existingPlants.filter { it in incompatibles }

        val message = buildString {
            append("Added $plantName to the garden. ")
            if (presentCompanions.isNotEmpty()) {
                append("Companions: ${presentCompanions.joinToString(", ")}. ")
            }
            if (presentIncompatibles.isNotEmpty()) {
                append("Incompatibles: ${presentIncompatibles.joinToString(", ")}.")
            }
        }
        Snackbar.make(gardenGrid, message, Snackbar.LENGTH_LONG).show()
    }

    private fun getPlantIconResource(plantName: String): Int {
        val normalizedName = plantName.lowercase(Locale.getDefault()).replace(" ", "_")
        return plantIconMap[normalizedName] ?: R.drawable.seed
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
        val plantImage = cell.findViewById<ImageView>(R.id.plant_image)
        plantImage.setImageDrawable(null)
        plantImage.tag = null
        cell.findViewById<View>(R.id.cell_overlay).setBackgroundColor(Color.TRANSPARENT)
        Toast.makeText(this, "Plant removed", Toast.LENGTH_SHORT).show()

        // Update visuals after removing a plant
        val existingPlants = getExistingPlants()
        updatePlantRelationshipVisuals(existingPlants)
    }

    private fun updateAllCellVisuals() {
        Log.d(TAG, "Updating all cell visuals")
        val existingPlants = getExistingPlants()
        Log.d(TAG, "Existing plants: $existingPlants")
        updatePlantRelationshipVisuals(existingPlants)
    }
}