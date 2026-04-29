package com.example.recipebookappandorid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.repository.AuthRepository
import com.example.recipebookappandorid.repository.RecipeRepository
import com.example.recipebookappandorid.repository.SharedRecipeBookRepository
import kotlinx.coroutines.launch

class MyRecipesViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val recipeRepository = RecipeRepository(application)
    private val sharedRecipeBookRepository = SharedRecipeBookRepository(application)
    private val currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()
    private val currentUserEmail = authRepository.getCurrentUser()?.email.orEmpty()
    private val allRecipes = recipeRepository.getAllRecipes()
    private val allBooks = sharedRecipeBookRepository.getCachedBooks()

    val myRecipes: LiveData<List<Recipe>> = MediatorLiveData<List<Recipe>>().apply {
        fun refresh() {
            val privateBookIds = allBooks.value.orEmpty()
                .filter { it.`private` && it.ownerId == currentUserId }
                .map { it.id }
                .toSet()

            value = allRecipes.value.orEmpty()
                .filter { recipe ->
                    recipe.authorId == currentUserId &&
                        (recipe.sharedBookId.isBlank() || privateBookIds.contains(recipe.sharedBookId))
                }
                .sortedByDescending { it.createdAt }
        }

        addSource(allRecipes) { refresh() }
        addSource(allBooks) { refresh() }
    }

    init {
        sync()
    }

    fun sync() {
        if (currentUserId.isBlank()) return

        viewModelScope.launch {
            runCatching {
                val privateBook = sharedRecipeBookRepository.ensurePrivateBook(currentUserId, currentUserEmail)
                sharedRecipeBookRepository.syncForUser(currentUserId, currentUserEmail)
                recipeRepository.syncRecipesForCurrentUser(currentUserId, currentUserEmail)
                val privateBookIds = sharedRecipeBookRepository
                    .getBooksForUser(currentUserId, currentUserEmail)
                    .filter { it.`private` && it.ownerId == currentUserId }
                    .map { it.id }
                    .toSet() + privateBook.id
                recipeRepository.removeDuplicateRecipesForBooks(currentUserId, privateBookIds)
            }
        }
    }
}
