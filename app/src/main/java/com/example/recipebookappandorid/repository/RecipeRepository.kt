package com.example.recipebookappandorid.repository

import android.content.Context
import com.example.recipebookappandorid.data.local.AppDatabase
import com.example.recipebookappandorid.data.local.entity.RecipeEntity
import com.example.recipebookappandorid.model.Recipe

class RecipeRepository(context: Context) {

    private val recipeDao = AppDatabase.getInstance(context).recipeDao()

    suspend fun saveRecipe(recipe: Recipe) {
        recipeDao.insertRecipe(recipe.toEntity())
    }

    suspend fun getAllRecipes(): List<Recipe> {
        return recipeDao.getAllRecipes().map { it.toModel() }
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
            authorId = authorId,
            authorName = authorName,
            createdAt = createdAt
        )
    }
}