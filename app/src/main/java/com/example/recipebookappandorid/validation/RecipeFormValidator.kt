package com.example.recipebookappandorid.validation

import com.example.recipebookappandorid.model.IngredientItem

data class RecipeValidationResult(
    val titleError: String? = null,
    val descriptionError: String? = null,
    val prepTimeError: String? = null,
    val difficultyError: String? = null,
    val categoryError: String? = null,
    val ingredientsError: String? = null,
    val stepsError: String? = null
) {
    val isValid: Boolean
        get() = listOf(
            titleError,
            descriptionError,
            prepTimeError,
            difficultyError,
            categoryError,
            ingredientsError,
            stepsError
        ).all { it == null }
}

object RecipeFormValidator {

    fun validate(
        title: String,
        description: String,
        prepTime: String,
        difficulty: String,
        category: String,
        ingredients: List<IngredientItem>,
        steps: String
    ): RecipeValidationResult {
        return RecipeValidationResult(
            titleError = if (title.isBlank()) "Title is required" else null,
            descriptionError = if (description.isBlank()) "Description is required" else null,
            prepTimeError = if (prepTime.isBlank()) "Prep time is required" else null,
            difficultyError = if (difficulty.isBlank()) "Difficulty is required" else null,
            categoryError = if (category.isBlank()) "Category is required" else null,
            ingredientsError = validateIngredients(ingredients),
            stepsError = if (steps.isBlank()) "Preparation steps are required" else null
        )
    }

    private fun validateIngredients(ingredients: List<IngredientItem>): String? {
        val normalized = ingredients.map {
            it.copy(
                name = it.name.trim(),
                quantity = it.quantity.trim(),
                unit = it.unit.trim()
            )
        }.filter {
            it.name.isNotBlank() || it.quantity.isNotBlank() || it.unit.isNotBlank()
        }

        if (normalized.isEmpty()) {
            return "At least one ingredient is required"
        }

        val invalidIngredient = normalized.firstOrNull {
            it.name.isBlank() || it.quantity.isBlank() || it.unit.isBlank()
        }

        return if (invalidIngredient != null) {
            "Each ingredient must include name, quantity and unit"
        } else {
            null
        }
    }
}
