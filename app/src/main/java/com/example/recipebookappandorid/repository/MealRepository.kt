package com.example.recipebookappandorid.repository

import com.example.recipebookappandorid.data.remote.RetrofitClient
import com.example.recipebookappandorid.data.remote.dto.MealDto
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.util.IngredientsCodec

class MealRepository {

    suspend fun getCategories(): List<String> {
        return runCatching {
            RetrofitClient.mealApiService.getCategories().categories.orEmpty()
                .mapNotNull { it.strCategory?.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(8)
        }.getOrElse {
            emptyList()
        }
    }

    suspend fun getStarterMeals(): List<Recipe> {
        val starterLetters = listOf("a", "b", "c")

        return starterLetters
            .flatMap { letter ->
                fetchMeals {
                    RetrofitClient.mealApiService.getStarterMeals(letter).meals.orEmpty()
                }
            }
            .distinctBy { it.id }
            .take(12)
    }

    suspend fun searchMeals(query: String): List<Recipe> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return emptyList()

        return fetchMeals {
            RetrofitClient.mealApiService.searchMealsByName(normalizedQuery).meals.orEmpty()
        }
    }

    private suspend fun fetchMeals(block: suspend () -> List<MealDto>): List<Recipe> {
        return runCatching {
            block().mapNotNull { it.toRecipe() }
        }.getOrElse {
            emptyList()
        }
    }

    private fun MealDto.toRecipe(): Recipe? {
        val id = idMeal ?: return null
        val title = strMeal?.trim().orEmpty()
        if (title.isBlank()) return null

        val ingredientItems = ingredientPairs()
            .mapNotNull { (ingredient, measure) ->
                val ingredientText = ingredient?.trim().orEmpty()
                if (ingredientText.isBlank()) {
                    null
                } else {
                    val measureText = measure?.trim().orEmpty()
                    val parts = measureText.split(" ", limit = 2)
                    IngredientsCodec.fromNameQuantityUnit(
                        name = ingredientText,
                        quantity = parts.firstOrNull().orEmpty(),
                        unit = parts.getOrNull(1).orEmpty()
                    )
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
            ingredients = IngredientsCodec.encode(ingredientItems),
            steps = strInstructions.orEmpty(),
            notes = strArea?.takeIf { it.isNotBlank() }?.let { "Cuisine: $it" }.orEmpty(),
            authorId = "themealdb",
            authorName = "TheMealDB",
            createdAt = 0L
        )
    }

    private fun MealDto.ingredientPairs(): List<Pair<String?, String?>> {
        return listOf(
            strIngredient1 to strMeasure1,
            strIngredient2 to strMeasure2,
            strIngredient3 to strMeasure3,
            strIngredient4 to strMeasure4,
            strIngredient5 to strMeasure5
        )
    }
}
