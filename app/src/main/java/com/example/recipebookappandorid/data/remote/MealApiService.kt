package com.example.recipebookappandorid.data.remote

import com.example.recipebookappandorid.data.remote.dto.MealListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MealApiService {

    @GET("search.php")
    suspend fun searchMeals(
        @Query("f") firstLetter: String
    ): MealListResponse
}
