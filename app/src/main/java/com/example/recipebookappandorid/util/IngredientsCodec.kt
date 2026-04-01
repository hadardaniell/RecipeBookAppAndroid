package com.example.recipebookappandorid.util

import com.example.recipebookappandorid.model.IngredientItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object IngredientsCodec {

    private val gson = Gson()
    private val listType = object : TypeToken<List<IngredientItem>>() {}.type

    fun encode(items: List<IngredientItem>): String {
        return gson.toJson(items.normalized())
    }

    fun decode(value: String): List<IngredientItem> {
        if (value.isBlank()) return emptyList()

        return runCatching {
            gson.fromJson<List<IngredientItem>>(value, listType)
                ?.normalized()
                .orEmpty()
        }.getOrElse {
            decodeLegacyText(value)
        }
    }

    fun toDisplayText(value: String): String {
        val items = decode(value)
        if (items.isEmpty()) return value

        return items.joinToString("\n") { item ->
            buildString {
                append(
                    listOf(item.quantity, item.unit, item.name)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                )
            }
        }
    }

    fun toSearchableText(value: String): String {
        val items = decode(value)
        if (items.isEmpty()) return value

        return items.joinToString(" ") { item ->
            listOf(item.name, item.quantity, item.unit)
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }
    }

    fun fromNameQuantityUnit(name: String, quantity: String, unit: String): IngredientItem {
        return IngredientItem(
            name = name.trim(),
            quantity = quantity.trim(),
            unit = unit.trim()
        )
    }

    private fun decodeLegacyText(value: String): List<IngredientItem> {
        return value.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                val normalizedLine = line.replace(Regex("\\s+"), " ").trim()
                val parts = normalizedLine.split(" ", limit = 3)
                when {
                    parts.size >= 3 -> IngredientItem(
                        quantity = parts[0],
                        unit = parts[1],
                        name = parts[2]
                    )

                    parts.size == 2 -> IngredientItem(
                        quantity = parts[0],
                        name = parts[1]
                    )

                    else -> IngredientItem(name = normalizedLine)
                }
            }
            .normalized()
    }

    private fun List<IngredientItem>.normalized(): List<IngredientItem> {
        return map {
            it.copy(
                name = it.name.trim(),
                quantity = it.quantity.trim(),
                unit = it.unit.trim()
            )
        }.filter {
            it.name.isNotBlank() || it.quantity.isNotBlank() || it.unit.isNotBlank()
        }
    }
}
