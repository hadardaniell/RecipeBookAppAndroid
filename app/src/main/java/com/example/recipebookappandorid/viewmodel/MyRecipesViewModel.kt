package com.example.recipebookappandorid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.repository.AuthRepository
import com.example.recipebookappandorid.repository.RecipeRepository

class MyRecipesViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val recipeRepository = RecipeRepository(application)
    private val currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()

    val myRecipes: LiveData<List<Recipe>> = recipeRepository.getAllRecipes().map { recipes ->
        recipes.filter { it.authorId == currentUserId }
            .sortedByDescending { it.createdAt }
    }
}
