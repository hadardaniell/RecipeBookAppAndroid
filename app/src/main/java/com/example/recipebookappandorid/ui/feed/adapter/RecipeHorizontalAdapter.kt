package com.example.recipebookappandorid.ui.feed.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.ItemRecipeBinding
import com.example.recipebookappandorid.model.Recipe

class RecipeHorizontalAdapter(
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeHorizontalAdapter.ViewHolder>() {

    private val recipes = mutableListOf<Recipe>()

    inner class ViewHolder(val binding: ItemRecipeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecipeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.binding.tvTitle.text = recipe.title
        holder.binding.tvDescription.text = recipe.cardSubtitle()
        holder.binding.tvAuthor.text = "By ${recipe.authorName}"

        Glide.with(holder.binding.ivRecipeImage)
            .load(recipe.imageUrl.ifBlank { null })
            .placeholder(R.mipmap.ic_launcher)
            .error(R.mipmap.ic_launcher)
            .into(holder.binding.ivRecipeImage)

        holder.itemView.setOnClickListener {
            onRecipeClick(recipe)
        }
    }

    override fun getItemCount(): Int = recipes.size

    fun submitList(newRecipes: List<Recipe>) {
        recipes.clear()
        recipes.addAll(newRecipes)
        notifyDataSetChanged()
    }

    private fun Recipe.cardSubtitle(): String {
        val metadata = listOf(difficulty, prepTime)
            .filter { it.isNotBlank() && it != "N/A" }

        return if (metadata.isNotEmpty()) {
            metadata.joinToString(" - ")
        } else {
            description.ifBlank { category }
        }
    }
}
