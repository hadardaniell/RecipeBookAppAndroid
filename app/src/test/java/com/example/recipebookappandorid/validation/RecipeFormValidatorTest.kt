package com.example.recipebookappandorid.validation

import com.example.recipebookappandorid.model.IngredientItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeFormValidatorTest {

    @Test
    fun `recipe validation rejects missing ingredients`() {
        val result = RecipeFormValidator.validate(
            title = "Pasta",
            description = "Creamy pasta",
            prepTime = "20 min",
            difficulty = "Easy",
            category = "Dinner",
            ingredients = emptyList(),
            steps = "Cook and serve"
        )

        assertEquals("At least one ingredient is required", result.ingredientsError)
        assertTrue(!result.isValid)
    }

    @Test
    fun `recipe validation rejects partial ingredient rows`() {
        val result = RecipeFormValidator.validate(
            title = "Pasta",
            description = "Creamy pasta",
            prepTime = "20 min",
            difficulty = "Easy",
            category = "Dinner",
            ingredients = listOf(
                IngredientItem(name = "Flour", quantity = "2", unit = "cups"),
                IngredientItem(name = "Milk", quantity = "", unit = "ml")
            ),
            steps = "Cook and serve"
        )

        assertEquals("Each ingredient must include name, quantity and unit", result.ingredientsError)
        assertTrue(!result.isValid)
    }

    @Test
    fun `recipe validation accepts fully structured ingredients`() {
        val result = RecipeFormValidator.validate(
            title = "Pasta",
            description = "Creamy pasta",
            prepTime = "20 min",
            difficulty = "Easy",
            category = "Dinner",
            ingredients = listOf(
                IngredientItem(name = "Flour", quantity = "2", unit = "cups"),
                IngredientItem(name = "Milk", quantity = "250", unit = "ml")
            ),
            steps = "Cook and serve"
        )

        assertTrue(result.isValid)
    }
}
