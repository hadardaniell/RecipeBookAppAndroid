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

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    fun addRecipe(title: String, description: String) {
        _titleError.value = null
        _descriptionError.value = null

        var isValid = true

        if (title.isBlank()) {
            _titleError.value = "Title is required"
            isValid = false
        }

        if (description.isBlank()) {
            _descriptionError.value = "Description is required"
            isValid = false
        }

        if (!isValid) return

        val firebaseUser = authRepository.getCurrentUser() ?: return
        val uid = firebaseUser.uid

        viewModelScope.launch {
            val currentUser = userRepository.getUser(uid)

            val recipe = Recipe(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                imageUrl = "",
                authorId = uid,
                authorName = currentUser?.name ?: "Unknown",
                createdAt = System.currentTimeMillis()
            )

            recipeRepository.saveRecipe(recipe)
            _saveSuccess.postValue(true)
        }
    }
}