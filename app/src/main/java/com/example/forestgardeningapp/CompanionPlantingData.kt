package com.example.forestgardeningapp

object CompanionPlantingData {
    val companionPlantingMap: Map<String, List<String>> = mapOf(
        "tomatoes" to listOf("basil", "mint", "oregano", "thyme"),
        "bell pepper" to listOf("basil", "oregano"),
        "basil" to listOf("tomatoes", "bell pepper"),
        "rosemary" to listOf("thyme", "oregano"),
        "thyme" to listOf("rosemary", "oregano"),
        "oregano" to listOf("tomatoes", "bell pepper", "rosemary", "thyme"),
        "mint" to listOf("tomatoes"),
        "strawberries" to listOf("lettuce", "green beans"),
        "lettuce" to listOf("strawberries", "cucumbers"),
        "green beans" to listOf("strawberries", "cucumbers", "eggplant"),
        "cucumbers" to listOf("lettuce", "green beans"),
        "eggplant" to listOf("green beans")
    )

    val incompatiblePlantingMap: Map<String, List<String>> = mapOf(
        "tomatoes" to listOf("potatoes", "eggplant"),
        "bell pepper" to listOf("beans"),
        "mint" to listOf("rosemary", "thyme"),  // Mint can be invasive, so keep it separate
        "strawberries" to listOf("tomatoes", "eggplant"),
        "potatoes" to listOf("tomatoes", "eggplant"),
        "cucumbers" to listOf("potatoes")
    )
}