package com.example.recipebookappandorid.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.recipebookappandorid.data.local.AppDatabase
import com.example.recipebookappandorid.data.local.entity.RecipeEntity
import com.example.recipebookappandorid.model.Recipe

class RecipeRepository(context: Context) {

    private val recipeDao = AppDatabase.getInstance(context).recipeDao()

    suspend fun saveRecipe(recipe: Recipe) {
        recipeDao.insertRecipe(recipe.toEntity())
    }

    fun getAllRecipes(): LiveData<List<Recipe>> {
        return recipeDao.getAllRecipes().map { list ->
            list.map { it.toModel() }
        }
    }

    suspend fun getMyRecipes(authorId: String): List<Recipe> {
        return recipeDao.getRecipesByAuthor(authorId).map { it.toModel() }
    }

    suspend fun getRecipeById(recipeId: String): Recipe? {
        return recipeDao.getRecipeById(recipeId)?.toModel()
    }

    suspend fun updateRecipe(recipe: Recipe) {
        recipeDao.updateRecipe(recipe.toEntity())
    }

    suspend fun deleteRecipe(recipeId: String) {
        recipeDao.deleteRecipeById(recipeId)
    }

    private fun Recipe.toEntity(): RecipeEntity {
        return RecipeEntity(
            id = id,
            title = title,
            description = description,
            imageUrl = imageUrl,
            prepTime = prepTime,
            difficulty = difficulty,
            category = category,
            ingredients = ingredients,
            steps = steps,
            notes = notes,
            authorId = authorId,
            authorName = authorName,
            createdAt = createdAt
        )
    }

    private fun RecipeEntity.toModel(): Recipe {
        return Recipe(
            id = id,
            title = title,
            description = description,
            imageUrl = imageUrl,
            prepTime = prepTime,
            difficulty = difficulty,
            category = category,
            ingredients = ingredients,
            steps = steps,
            notes = notes,
            authorId = authorId,
            authorName = authorName,
            createdAt = createdAt
        )
    }
}