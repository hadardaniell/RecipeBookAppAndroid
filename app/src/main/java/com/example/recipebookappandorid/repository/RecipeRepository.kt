package com.example.recipebookappandorid.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.recipebookappandorid.data.local.AppDatabase
import com.example.recipebookappandorid.data.local.entity.RecipeEntity
import com.example.recipebookappandorid.model.Recipe
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RecipeRepository(context: Context) {

    private val recipeDao = AppDatabase.getInstance(context).recipeDao()
    private val recipesCollection = FirebaseFirestore.getInstance().collection("recipes")

    suspend fun saveRecipe(recipe: Recipe) {
        recipeDao.insertRecipe(recipe.toEntity())
        recipesCollection.document(recipe.id).set(recipe).await()
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
        recipesCollection.document(recipe.id).set(recipe).await()
    }

    suspend fun deleteRecipe(recipeId: String) {
        recipeDao.deleteRecipeById(recipeId)
        recipesCollection.document(recipeId).delete().await()
    }

    suspend fun syncUserRecipesFromCloud(authorId: String) {
        val snapshot = recipesCollection
            .whereEqualTo("authorId", authorId)
            .get()
            .await()

        val recipes = snapshot.documents.mapNotNull { document ->
            document.toObject(Recipe::class.java)
        }

        recipeDao.clearRecipes()
        recipeDao.insertRecipes(recipes.map { it.toEntity() })
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
