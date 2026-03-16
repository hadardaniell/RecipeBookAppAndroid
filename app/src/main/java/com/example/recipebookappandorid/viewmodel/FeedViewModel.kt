package com.example.recipebookappandorid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.model.RecipeSection
import com.example.recipebookappandorid.repository.MealRepository
import com.example.recipebookappandorid.repository.RecipeRepository
import kotlinx.coroutines.launch

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecipeRepository(application)
    private val mealRepository = MealRepository()
    private val allRecipes = repository.getAllRecipes()

    private var remoteRecipes: List<Recipe> = emptyList()
    private var currentQuery: String = ""
    private var selectedCategory: String? = null

    val sections = MediatorLiveData<List<RecipeSection>>().apply {
        addSource(allRecipes) { recipes ->
            value = buildSections(
                filterRecipes(recipes + remoteRecipes, currentQuery, selectedCategory)
            )
        }
    }

    init {
        loadRemoteMeals()
    }

    fun setSearchQuery(query: String) {
        currentQuery = query
        sections.value = buildSections(
            filterRecipes(
                allRecipes.value.orEmpty() + remoteRecipes,
                currentQuery,
                selectedCategory
            )
        )
    }

    fun setCategoryFilter(category: String?) {
        selectedCategory = category
        sections.value = buildSections(
            filterRecipes(
                allRecipes.value.orEmpty() + remoteRecipes,
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

        val importedMeals = recipes.filter { it.authorId == "themealdb" }
        if (importedMeals.isNotEmpty()) {
            sections.add(
                RecipeSection(
                    title = "From TheMealDB",
                    recipes = importedMeals.take(10)
                )
            )
        }

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

        recipes.groupBy { it.category.ifBlank { "Other" } }
            .forEach { (category, categoryRecipes) ->
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

    private fun loadRemoteMeals() {
        viewModelScope.launch {
            remoteRecipes = mealRepository.getStarterMeals()
            sections.postValue(
                buildSections(
                    filterRecipes(
                        allRecipes.value.orEmpty() + remoteRecipes,
                        currentQuery,
                        selectedCategory
                    )
                )
            )
        }
    }

    private fun extractMinutes(prepTime: String): Int {
        val number = Regex("\\d+").find(prepTime)?.value
        return number?.toIntOrNull() ?: Int.MAX_VALUE
    }
}
