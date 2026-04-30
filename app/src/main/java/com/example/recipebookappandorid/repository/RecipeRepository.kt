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
        return recipeDao.getAllRecipes().map { list -> list.map { it.toModel() } }
    }

    fun getRecentlyViewedRecipes(limit: Int = 10): LiveData<List<Recipe>> {
        return recipeDao.getRecentlyViewedRecipes(limit).map { list -> list.map { it.toModel() } }
    }

    suspend fun getMyRecipes(authorId: String): List<Recipe> {
        return recipeDao.getRecipesByAuthor(authorId).map { it.toModel() }
    }

    suspend fun removeDuplicateRecipesForBooks(authorId: String, bookIds: Set<String>) {
        if (bookIds.isEmpty()) return

        val duplicates = recipeDao.getRecipesByAuthor(authorId)
            .map { it.toModel() }
            .filter { it.sharedBookId in bookIds }
            .groupBy { recipe ->
                val canonicalSource = recipe.sourceRecipeId
                    .ifBlank { recipe.title.trim().lowercase() }
                "${recipe.sharedBookId}|$canonicalSource"
            }
            .values
            .flatMap { recipes ->
                recipes.sortedByDescending { it.createdAt }.drop(1)
            }

        duplicates.forEach { duplicate ->
            deleteRecipe(duplicate.id)
        }
    }

    suspend fun getRecipeById(recipeId: String): Recipe? {
        return recipeDao.getRecipeById(recipeId)?.toModel()
    }

    suspend fun recipeExistsInAnyBook(sourceRecipeId: String, title: String, excludeRecipeId: String = ""): Boolean {
        val candidates = buildList {
            if (sourceRecipeId.isNotBlank()) {
                addAll(recipeDao.getRecipesBySourceRecipeId(sourceRecipeId))
            }
            if (isEmpty()) {
                addAll(recipeDao.getRecipesByTitle(title))
            }
        }
        return candidates.any { it.id != excludeRecipeId }
    }

    suspend fun updateRecipe(recipe: Recipe) {
        recipeDao.updateRecipe(recipe.toEntity())
        recipesCollection.document(recipe.id).set(recipe).await()
    }

    suspend fun deleteRecipe(recipeId: String) {
        recipeDao.deleteRecipeById(recipeId)
        recipesCollection.document(recipeId).delete().await()
    }

    suspend fun markRecipeViewed(recipeId: String, viewedAt: Long = System.currentTimeMillis()) {
        recipeDao.updateLastViewedAt(recipeId, viewedAt)
    }

    suspend fun recordRecipeViewed(recipe: Recipe, viewedAt: Long = System.currentTimeMillis()) {
        val existingRecipe = recipeDao.getRecipeById(recipe.id)
        if (existingRecipe != null) {
            recipeDao.updateLastViewedAt(recipe.id, viewedAt)
            return
        }

        recipeDao.insertRecipe(recipe.copy(lastViewedAt = viewedAt).toEntity())
    }

    suspend fun shareRecipeWithEmail(recipeId: String, email: String) {
        val normalizedEmail = email.trim().lowercase()
        recipesCollection.document(recipeId)
            .update("sharedWith", FieldValue.arrayUnion(normalizedEmail))
            .await()

        val localRecipe = getRecipeById(recipeId)
        if (localRecipe != null && !localRecipe.sharedWith.contains(normalizedEmail)) {
            val updated = localRecipe.copy(sharedWith = localRecipe.sharedWith + listOf(normalizedEmail))
            recipeDao.updateRecipe(updated.toEntity())
        }
    }

    suspend fun syncSharedRecipesFromCloud(userId: String, userEmail: String) {
        try {
            val ownRecipes = recipesCollection
                .whereEqualTo("authorId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Recipe::class.java) }

            val sharedByUserId = recipesCollection
                .whereArrayContains("sharedWithUserIds", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Recipe::class.java) }

            val sharedByEmail = if (userEmail.isNotBlank()) {
                recipesCollection
                    .whereArrayContains("sharedWith", userEmail.lowercase())
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(Recipe::class.java) }
            } else {
                emptyList()
            }

            val existingViewedTimestamps = recipeDao.getAllRecipesOnce()
                .associate { it.id to it.lastViewedAt }

            val combined = (ownRecipes + sharedByUserId + sharedByEmail)
                .distinctBy { it.id }
                .map { recipe ->
                    recipe.copy(lastViewedAt = existingViewedTimestamps[recipe.id] ?: recipe.lastViewedAt)
                }
            
            recipeDao.insertRecipes(combined.map { it.toEntity() })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncRecipesForCurrentUser(userId: String, userEmail: String) {
        syncSharedRecipesFromCloud(userId, userEmail)
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
            sourceRecipeId = sourceRecipeId,
            authorId = authorId,
            authorName = authorName,
            sharedBookId = sharedBookId,
            sharedBookName = sharedBookName,
            sharedWithUserIds = sharedWithUserIds,
            sharedRole = sharedRole,
            createdAt = createdAt,
            lastViewedAt = lastViewedAt,
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
            sourceRecipeId = sourceRecipeId,
            authorId = authorId,
            authorName = authorName,
            sharedBookId = sharedBookId,
            sharedBookName = sharedBookName,
            sharedWithUserIds = sharedWithUserIds,
            sharedRole = sharedRole,
            createdAt = createdAt,
            lastViewedAt = lastViewedAt,
            sharedWith = if (sharedWith.isBlank()) emptyList() else sharedWith.split(",")
        )
    }
}
