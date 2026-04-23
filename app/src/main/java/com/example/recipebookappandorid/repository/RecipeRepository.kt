package com.example.recipebookappandorid.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.recipebookappandorid.data.local.AppDatabase
import com.example.recipebookappandorid.data.local.entity.RecipeEntity
import com.example.recipebookappandorid.model.Recipe
import com.google.firebase.firestore.FieldValue
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

    suspend fun shareRecipeWithEmail(recipeId: String, email: String) {
        // Add the email to the sharedWith array in Firestore
        recipesCollection.document(recipeId).update("sharedWith", FieldValue.arrayUnion(email)).await()
        
        // Update local room database
        val localRecipe = getRecipeById(recipeId)
        if (localRecipe != null) {
            val updatedList = localRecipe.sharedWith.toMutableList()
            if (!updatedList.contains(email)) {
                updatedList.add(email)
                updateRecipe(localRecipe.copy(sharedWith = updatedList))
            }
        }
    }

    suspend fun syncAllRecipesFromCloud(currentUserEmail: String) {
        try {
            // Get Community Recipes (You can limit this or grab all depending on your app rules. Here we fetch globally shared or something similar. 
            // For this requirement, let's fetch recipes shared explicitly with the user, and maybe general community ones).
            
            // 1. Fetch recipes shared explicitly with this user's email
            val sharedSnapshot = recipesCollection
                .whereArrayContains("sharedWith", currentUserEmail)
                .get()
                .await()

            // 2. Fetch all community recipes (Optional, but let's keep it to have a populated feed)
            val communitySnapshot = recipesCollection
                .limit(20) // Limit to avoid massive downloads
                .get()
                .await()

            val sharedRecipes = sharedSnapshot.documents.mapNotNull { it.toObject(Recipe::class.java) }
            val communityRecipes = communitySnapshot.documents.mapNotNull { it.toObject(Recipe::class.java) }

            val allFetched = (sharedRecipes + communityRecipes).distinctBy { it.id }

            if (allFetched.isNotEmpty()) {
                recipeDao.insertRecipes(allFetched.map { it.toEntity() })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncUserRecipesFromCloud(authorId: String) {
        try {
            val snapshot = recipesCollection
                .whereEqualTo("authorId", authorId)
                .get()
                .await()

            val recipes = snapshot.documents.mapNotNull { document ->
                document.toObject(Recipe::class.java)
            }

            val currentRecipes = recipeDao.getRecipesByAuthor(authorId)
            currentRecipes.forEach { recipeDao.deleteRecipeById(it.id) }
            
            recipeDao.insertRecipes(recipes.map { it.toEntity() })
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            createdAt = createdAt,
            sharedWith = sharedWith.joinToString(",")
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
            createdAt = createdAt,
            sharedWith = if (sharedWith.isBlank()) emptyList() else sharedWith.split(",")
        )
    }
}
