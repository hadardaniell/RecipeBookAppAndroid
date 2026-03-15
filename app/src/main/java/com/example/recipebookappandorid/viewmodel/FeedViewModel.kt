package com.example.recipebookappandorid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.model.RecipeSection
import com.example.recipebookappandorid.repository.RecipeRepository

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecipeRepository(application)
    private val allRecipes = repository.getAllRecipes()

    private var currentQuery: String = ""
    private var selectedCategory: String? = null

    val sections = MediatorLiveData<List<RecipeSection>>().apply {
        addSource(allRecipes) { recipes ->
            value = buildSections(filterRecipes(recipes, currentQuery, selectedCategory))
        }
    }

    fun setSearchQuery(query: String) {
        currentQuery = query
        sections.value = buildSections(
            filterRecipes(
                allRecipes.value.orEmpty(),
                currentQuery,
                selectedCategory
            )
        )
    }

    fun setCategoryFilter(category: String?) {
        selectedCategory = category
        sections.value = buildSections(
            filterRecipes(
                allRecipes.value.orEmpty(),
                currentQuery,
                selectedCategory
            )
        )
    }

    private fun filterRecipes(
        recipes: List<Recipe>,
        query: String,
        category: String?
    ): List<Recipe> {
        return recipes.filter { recipe ->
            val matchesQuery =
                query.isBlank() ||
                        recipe.title.contains(query, ignoreCase = true) ||
                        recipe.category.contains(query, ignoreCase = true) ||
                        recipe.ingredients.contains(query, ignoreCase = true)

            val matchesCategory =
                category.isNullOrBlank() ||
                        recipe.category.equals(category, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }

    private fun buildSections(recipes: List<Recipe>): List<RecipeSection> {
        if (recipes.isEmpty()) return emptyList()

        val sections = mutableListOf<RecipeSection>()

        sections.add(
            RecipeSection(
                title = "Popular now",
                recipes = recipes.take(10)
            )
        )

        val easyRecipes = recipes.filter {
            it.difficulty.equals("Easy", ignoreCase = true)
        }
        if (easyRecipes.isNotEmpty()) {
            sections.add(
                RecipeSection(
                    title = "Easy to make",
                    recipes = easyRecipes.take(10)
                )
            )
        }

        val quickRecipes = recipes.filter { recipe ->
            extractMinutes(recipe.prepTime) <= 30
        }
        if (quickRecipes.isNotEmpty()) {
            sections.add(
                RecipeSection(
                    title = "Ready in 30 min",
                    recipes = quickRecipes.take(10)
                )
            )
        }

        val groupedByCategory = recipes
            .groupBy { it.category.ifBlank { "Other" } }

        groupedByCategory.forEach { (category, categoryRecipes) ->
            if (categoryRecipes.isNotEmpty()) {
                sections.add(
                    RecipeSection(
                        title = category,
                        recipes = categoryRecipes.take(10)
                    )
                )
            }
        }

        return sections.distinctBy { it.title }
    }

    private fun extractMinutes(prepTime: String): Int {
        val number = Regex("\\d+").find(prepTime)?.value
        return number?.toIntOrNull() ?: Int.MAX_VALUE
    }
}