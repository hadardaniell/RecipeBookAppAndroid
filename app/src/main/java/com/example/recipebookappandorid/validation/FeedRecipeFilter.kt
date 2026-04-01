package com.example.recipebookappandorid.validation

import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.util.IngredientsCodec

object FeedRecipeFilter {

    fun filter(
        recipes: List<Recipe>,
        query: String,
        category: String?
    ): List<Recipe> {
        return recipes.filter { recipe ->
            val matchesQuery =
                query.isBlank() ||
                    recipe.title.contains(query, ignoreCase = true) ||
                    recipe.category.contains(query, ignoreCase = true) ||
                    IngredientsCodec.toSearchableText(recipe.ingredients)
                        .contains(query, ignoreCase = true)

            val matchesCategory =
                category.isNullOrBlank() ||
                    recipe.category.equals(category, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }
}
