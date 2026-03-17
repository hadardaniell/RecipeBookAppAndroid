package com.example.recipebookappandorid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.model.RecipeSection
import com.example.recipebookappandorid.repository.AuthRepository
import com.example.recipebookappandorid.repository.MealRepository
import com.example.recipebookappandorid.repository.RecipeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecipeRepository(application)
    private val authRepository = AuthRepository()
    private val mealRepository = MealRepository()
    private val allRecipes = repository.getAllRecipes()

    private var remoteRecipes: List<Recipe> = emptyList()
    private var currentQuery: String = ""
    private var selectedCategory: String? = null
    private var remoteSearchJob: Job? = null
    private val _categories = MutableLiveData<List<String>>(emptyList())
    val categories: LiveData<List<String>> = _categories

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _emptyStateMessage = MutableLiveData<String?>(null)
    val emptyStateMessage: LiveData<String?> = _emptyStateMessage

    val sections = MediatorLiveData<List<RecipeSection>>().apply {
        addSource(allRecipes) { recipes ->
            value = buildSections(
                filterRecipes(recipes + remoteRecipes, currentQuery, selectedCategory)
            )
            updateEmptyState(value.orEmpty())
        }
    }

    init {
        syncCloudRecipes()
        loadCategories()
        loadStarterMeals()
    }

    fun setSearchQuery(query: String) {
        currentQuery = query.trim()
        refreshSections()
        searchRemoteMeals()
    }

    fun setCategoryFilter(category: String?) {
        selectedCategory = category
        refreshSections()
    }

    private fun searchRemoteMeals() {
        remoteSearchJob?.cancel()
        remoteSearchJob = viewModelScope.launch {
            _isLoading.postValue(true)
            if (currentQuery.isBlank()) {
                remoteRecipes = mealRepository.getStarterMeals()
                refreshSections()
                _isLoading.postValue(false)
                updateEmptyState(sections.value.orEmpty())
                return@launch
            }

            delay(350)
            remoteRecipes = mealRepository.searchMeals(currentQuery)
            refreshSections()
            _isLoading.postValue(false)
            updateEmptyState(sections.value.orEmpty())
        }
    }

    private fun loadStarterMeals() {
        remoteSearchJob?.cancel()
        viewModelScope.launch {
            _isLoading.postValue(true)
            remoteRecipes = mealRepository.getStarterMeals()
            refreshSections()
            _isLoading.postValue(false)
            updateEmptyState(sections.value.orEmpty())
        }
    }

    private fun refreshSections() {
        val newSections = buildSections(
            filterRecipes(
                allRecipes.value.orEmpty() + remoteRecipes,
                currentQuery,
                selectedCategory
            )
        )
        sections.value = newSections
        updateEmptyState(newSections)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _categories.postValue(mealRepository.getCategories())
        }
    }

    private fun syncCloudRecipes() {
        val user = authRepository.getCurrentUser() ?: return

        viewModelScope.launch {
            runCatching {
                repository.syncUserRecipesFromCloud(user.uid)
            }
        }
    }

    private fun updateEmptyState(sections: List<RecipeSection>) {
        _emptyStateMessage.value = when {
            _isLoading.value == true -> null
            sections.isNotEmpty() -> null
            currentQuery.isNotBlank() -> "No recipes found for \"$currentQuery\""
            else -> "No recipes available yet"
        }
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
                    title = if (currentQuery.isBlank()) "From TheMealDB" else "Search results",
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

    private fun extractMinutes(prepTime: String): Int {
        val number = Regex("\\d+").find(prepTime)?.value
        return number?.toIntOrNull() ?: Int.MAX_VALUE
    }
}
