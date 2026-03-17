package com.example.recipebookappandorid.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MealCategoryDto(
    @SerializedName("strCategory") val strCategory: String?
)
