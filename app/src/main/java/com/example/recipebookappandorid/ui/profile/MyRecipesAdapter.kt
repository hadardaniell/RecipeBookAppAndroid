package com.example.recipebookappandorid.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebookappandorid.databinding.ItemMyRecipeBinding
import com.example.recipebookappandorid.model.Recipe

class MyRecipesAdapter(
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<MyRecipesAdapter.ViewHolder>() {

    private val recipes = mutableListOf<Recipe>()

    inner class ViewHolder(val binding: ItemMyRecipeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyRecipeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.binding.tvRecipeTitle.text = recipe.title
        holder.binding.tvRecipeMeta.text = listOf(recipe.category, recipe.prepTime)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
        holder.binding.tvRecipeDescription.text = recipe.description.ifBlank { recipe.notes }
        holder.itemView.setOnClickListener { onRecipeClick(recipe) }
    }

    override fun getItemCount(): Int = recipes.size

    fun submitList(newRecipes: List<Recipe>) {
        recipes.clear()
        recipes.addAll(newRecipes)
        notifyDataSetChanged()
    }
}
