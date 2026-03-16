package com.example.recipebookappandorid.repository

import com.example.recipebookappandorid.data.remote.RetrofitClient
import com.example.recipebookappandorid.data.remote.dto.MealDto
import com.example.recipebookappandorid.model.Recipe

class MealRepository {

    suspend fun getStarterMeals(): List<Recipe> {
        return runCatching {
            val meals = RetrofitClient.mealApiService.searchMeals("a").meals.orEmpty()
            meals.mapNotNull { it.toRecipe() }.take(12)
        }.getOrElse {
            emptyList()
        }
    }

    private fun MealDto.toRecipe(): Recipe? {
        val id = idMeal ?: return null
        val title = strMeal?.trim().orEmpty()

        if (title.isBlank()) return null

        val ingredientLines = listOf(
            strIngredient1 to strMeasure1,
            strIngredient2 to strMeasure2,
            strIngredient3 to strMeasure3,
            strIngredient4 to strMeasure4,
            strIngredient5 to strMeasure5
        ).mapNotNull { (ingredient, measure) ->
            val ingredientText = ingredient?.trim().orEmpty()
            if (ingredientText.isBlank()) {
                null
            } else {
                listOf(measure?.trim().orEmpty(), ingredientText)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .trim()
            }
        }

        return Recipe(
            id = "remote_$id",
            title = title,
            description = strTags?.takeIf { it.isNotBlank() } ?: "Imported from TheMealDB",
            imageUrl = strMealThumb.orEmpty(),
            prepTime = "N/A",
            difficulty = "N/A",
            category = strCategory?.takeIf { it.isNotBlank() } ?: "Imported",
            ingredients = ingredientLines.joinToString("\n"),
            steps = strInstructions.orEmpty(),
            notes = strArea?.takeIf { it.isNotBlank() }?.let { "Cuisine: $it" }.orEmpty(),
            authorId = "themealdb",
            authorName = "TheMealDB",
            createdAt = 0L
        )
    }
}
