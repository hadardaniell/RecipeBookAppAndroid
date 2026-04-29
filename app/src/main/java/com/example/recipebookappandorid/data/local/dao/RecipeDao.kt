package com.example.recipebookappandorid.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.recipebookappandorid.data.local.entity.RecipeEntity

@Dao
interface RecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<RecipeEntity>)

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getAllRecipes(): LiveData<List<RecipeEntity>>

    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    suspend fun getAllRecipesOnce(): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE lastViewedAt > 0 ORDER BY lastViewedAt DESC LIMIT :limit")
    fun getRecentlyViewedRecipes(limit: Int = 10): LiveData<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE authorId = :authorId ORDER BY createdAt DESC")
    suspend fun getRecipesByAuthor(authorId: String): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE id = :recipeId LIMIT 1")
    suspend fun getRecipeById(recipeId: String): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE sourceRecipeId = :sourceRecipeId")
    suspend fun getRecipesBySourceRecipeId(sourceRecipeId: String): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE title = :title")
    suspend fun getRecipesByTitle(title: String): List<RecipeEntity>

    @Query("DELETE FROM recipes WHERE id = :recipeId")
    suspend fun deleteRecipeById(recipeId: String)

    @Query("UPDATE recipes SET lastViewedAt = :viewedAt WHERE id = :recipeId")
    suspend fun updateLastViewedAt(recipeId: String, viewedAt: Long)

    @Query("DELETE FROM recipes")
    suspend fun clearRecipes()
}
