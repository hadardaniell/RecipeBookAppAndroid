package com.example.recipebookappandorid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.recipebookappandorid.model.Recipe

class FeedViewModel : ViewModel() {

    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    fun loadRecipes() {

        val dummyRecipes = listOf(
            Recipe(
                title = "Creamy Mushroom Pasta",
                imageUrl = "",
                prepTime = "20 min",
                authorName = "Asaf"
            ),
            Recipe(
                title = "Shakshuka",
                imageUrl = "",
                prepTime = "15 min",
                authorName = "Hadar"
            )
        )

        _recipes.value = dummyRecipes
    }
}