package com.example.recipebookappandorid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.recipebookappandorid.data.local.dao.SharedBookInviteDao
import com.example.recipebookappandorid.data.local.dao.RecipeDao
import com.example.recipebookappandorid.data.local.dao.SharedRecipeBookDao
import com.example.recipebookappandorid.data.local.dao.UserDao
import com.example.recipebookappandorid.data.local.entity.RecipeEntity
import com.example.recipebookappandorid.data.local.entity.SharedBookInviteEntity
import com.example.recipebookappandorid.data.local.entity.SharedRecipeBookEntity
import com.example.recipebookappandorid.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        RecipeEntity::class,
        SharedRecipeBookEntity::class,
        SharedBookInviteEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun recipeDao(): RecipeDao
    abstract fun sharedRecipeBookDao(): SharedRecipeBookDao
    abstract fun sharedBookInviteDao(): SharedBookInviteDao

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
