package com.example.recipebookappandorid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.repository.AuthRepository
import com.example.recipebookappandorid.repository.RecipeRepository
import com.example.recipebookappandorid.repository.UserRepository
import kotlinx.coroutines.launch
import java.util.UUID

class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val recipeRepository = RecipeRepository(application)
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository(application)

    private val _titleError = MutableLiveData<String?>()
    val titleError: LiveData<String?> = _titleError

    private val _descriptionError = MutableLiveData<String?>()
    val descriptionError: LiveData<String?> = _descriptionError

    private val _prepTimeError = MutableLiveData<String?>()
    val prepTimeError: LiveData<String?> = _prepTimeError

    private val _difficultyError = MutableLiveData<String?>()
    val difficultyError: LiveData<String?> = _difficultyError

    private val _categoryError = MutableLiveData<String?>()
    val categoryError: LiveData<String?> = _categoryError

    private val _ingredientsError = MutableLiveData<String?>()
    val ingredientsError: LiveData<String?> = _ingredientsError

    private val _stepsError = MutableLiveData<String?>()
    val stepsError: LiveData<String?> = _stepsError

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _importSuccess = MutableLiveData<Boolean>()
    val importSuccess: LiveData<Boolean> = _importSuccess

    private val _saveError = MutableLiveData<String?>()
    val saveError: LiveData<String?> = _saveError

    private val _savedRecipe = MutableLiveData<Recipe?>()
    val savedRecipe: LiveData<Recipe?> = _savedRecipe

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    fun addRecipe(
        title: String,
        description: String,
        prepTime: String,
        difficulty: String,
        category: String,
        ingredients: String,
        steps: String,
        notes: String
    ) {
        _titleError.value = null
        _descriptionError.value = null
        _prepTimeError.value = null
        _difficultyError.value = null
        _categoryError.value = null
        _ingredientsError.value = null
        _stepsError.value = null
        _saveError.value = null
        _savedRecipe.value = null

        var isValid = true

        if (title.isBlank()) {
            _titleError.value = "Title is required"
            isValid = false
        }

        if (description.isBlank()) {
            _descriptionError.value = "Description is required"
            isValid = false
        }

        if (prepTime.isBlank()) {
            _prepTimeError.value = "Prep time is required"
            isValid = false
        }

        if (difficulty.isBlank()) {
            _difficultyError.value = "Difficulty is required"
            isValid = false
        }

        if (category.isBlank()) {
            _categoryError.value = "Category is required"
            isValid = false
        }

        if (ingredients.isBlank()) {
            _ingredientsError.value = "Ingredients are required"
            isValid = false
        }

        if (steps.isBlank()) {
            _stepsError.value = "Preparation steps are required"
            isValid = false
        }

        if (!isValid) return

        val firebaseUser = authRepository.getCurrentUser()
        if (firebaseUser == null) {
            _saveError.value = "You must be logged in to save a recipe"
            return
        }
        val uid = firebaseUser.uid

        viewModelScope.launch {
            val currentUser = userRepository.getUser(uid)

            val recipe = Recipe(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                imageUrl = "",
                prepTime = prepTime,
                difficulty = difficulty,
                category = category,
                ingredients = ingredients,
                steps = steps,
                notes = notes,
                authorId = uid,
                authorName = currentUser?.name ?: firebaseUser.email ?: "Unknown",
                createdAt = System.currentTimeMillis()
            )

            runCatching {
                recipeRepository.saveRecipe(recipe)
            }.onSuccess {
                _savedRecipe.postValue(recipe)
                _saveSuccess.postValue(true)
            }.onFailure { exception ->
                _saveError.postValue(exception.message ?: "Failed to save recipe")
            }
        }
    }

    fun importRecipe(recipe: Recipe) {
        val firebaseUser = authRepository.getCurrentUser()
        if (firebaseUser == null) {
            _saveError.value = "You must be logged in to import a recipe"
            return
        }
        val uid = firebaseUser.uid
        _saveError.value = null

        viewModelScope.launch {
            val currentUser = userRepository.getUser(uid)
            val importedRecipe = recipe.copy(
                id = UUID.randomUUID().toString(),
                authorId = uid,
                authorName = currentUser?.name ?: firebaseUser.email ?: "My Recipe",
                createdAt = System.currentTimeMillis()
            )

            runCatching {
                recipeRepository.saveRecipe(importedRecipe)
            }.onSuccess {
                _importSuccess.postValue(true)
            }.onFailure { exception ->
                _saveError.postValue(exception.message ?: "Failed to import recipe")
            }
        }
    }

    fun onRecipeNavigationHandled() {
        _savedRecipe.value = null
    }

    fun updateRecipe(
        recipeId: String,
        title: String,
        description: String,
        imageUrl: String,
        prepTime: String,
        difficulty: String,
        category: String,
        ingredients: String,
        steps: String,
        notes: String
    ) {
        _saveError.value = null
        _savedRecipe.value = null

        val firebaseUser = authRepository.getCurrentUser()
        if (firebaseUser == null) {
            _saveError.value = "You must be logged in to update a recipe"
            return
        }

        viewModelScope.launch {
            val currentUser = userRepository.getUser(firebaseUser.uid)
            val recipe = Recipe(
                id = recipeId,
                title = title,
                description = description,
                imageUrl = imageUrl,
                prepTime = prepTime,
                difficulty = difficulty,
                category = category,
                ingredients = ingredients,
                steps = steps,
                notes = notes,
                authorId = firebaseUser.uid,
                authorName = currentUser?.name ?: firebaseUser.email ?: "My Recipe",
                createdAt = System.currentTimeMillis()
            )

            runCatching {
                recipeRepository.updateRecipe(recipe)
            }.onSuccess {
                _savedRecipe.postValue(recipe)
                _saveSuccess.postValue(true)
            }.onFailure { exception ->
                _saveError.postValue(exception.message ?: "Failed to update recipe")
            }
        }
    }

    fun deleteRecipe(recipeId: String) {
        _saveError.value = null

        viewModelScope.launch {
            runCatching {
                recipeRepository.deleteRecipe(recipeId)
            }.onSuccess {
                _deleteSuccess.postValue(true)
            }.onFailure { exception ->
                _saveError.postValue(exception.message ?: "Failed to delete recipe")
            }
        }
    }
}
