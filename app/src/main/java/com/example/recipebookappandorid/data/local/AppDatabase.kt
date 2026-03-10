package com.example.recipebookappandorid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.recipebookappandorid.data.local.dao.RecipeDao
import com.example.recipebookappandorid.data.local.dao.UserDao
import com.example.recipebookappandorid.data.local.entity.RecipeEntity
import com.example.recipebookappandorid.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, RecipeEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recipe_app_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}