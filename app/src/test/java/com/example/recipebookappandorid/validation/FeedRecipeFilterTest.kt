package com.example.recipebookappandorid.validation

import com.example.recipebookappandorid.model.IngredientItem
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.util.IngredientsCodec
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedRecipeFilterTest {

    private val recipes = listOf(
        Recipe(
            id = "1",
            title = "Tomato Pasta",
            category = "Dinner",
            ingredients = IngredientsCodec.encode(
                listOf(
                    IngredientItem(name = "Tomato", quantity = "2", unit = "pcs"),
                    IngredientItem(name = "Pasta", quantity = "200", unit = "g")
                )
            )
        ),
        Recipe(
            id = "2",
            title = "Chocolate Cake",
            category = "Dessert",
            ingredients = IngredientsCodec.encode(
                listOf(
                    IngredientItem(name = "Chocolate", quantity = "100", unit = "g")
                )
            )
        )
    )

    @Test
    fun `filter returns recipes by category`() {
        val filtered = FeedRecipeFilter.filter(
            recipes = recipes,
            query = "",
            category = "Dessert"
        )

        assertEquals(listOf("2"), filtered.map { it.id })
    }

    @Test
    fun `filter returns recipes by ingredient query`() {
        val filtered = FeedRecipeFilter.filter(
            recipes = recipes,
            query = "tomato",
            category = null
        )

        assertEquals(listOf("1"), filtered.map { it.id })
    }

    @Test
    fun `filter combines query and category`() {
        val filtered = FeedRecipeFilter.filter(
            recipes = recipes,
            query = "chocolate",
            category = "Dessert"
        )

        assertEquals(listOf("2"), filtered.map { it.id })
    }
}
