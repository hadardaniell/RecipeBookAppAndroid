package com.example.recipebookappandorid.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.recipebookappandorid.data.local.entity.SharedRecipeBookEntity

@Dao
interface SharedRecipeBookDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<SharedRecipeBookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: SharedRecipeBookEntity)

    @Query("SELECT * FROM shared_books ORDER BY createdAt DESC")
    fun getAllBooks(): LiveData<List<SharedRecipeBookEntity>>

    @Query("SELECT * FROM shared_books ORDER BY createdAt DESC")
    suspend fun getAllBooksOnce(): List<SharedRecipeBookEntity>

    @Query("SELECT * FROM shared_books WHERE id = :bookId LIMIT 1")
    suspend fun getBookById(bookId: String): SharedRecipeBookEntity?

    @Query("DELETE FROM shared_books")
    suspend fun clearBooks()
}
