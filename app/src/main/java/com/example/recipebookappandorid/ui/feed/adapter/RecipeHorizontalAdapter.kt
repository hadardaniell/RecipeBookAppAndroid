package com.example.recipebookappandorid.ui.feed.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebookappandorid.databinding.ItemRecipeBinding
import com.example.recipebookappandorid.model.Recipe

class RecipeHorizontalAdapter(
    private val recipes: List<Recipe>,
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeHorizontalAdapter.ViewHolder>() {

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
        holder.binding.tvDescription.text = recipe.description
        holder.binding.tvAuthor.text = "By ${recipe.authorName}"

        holder.itemView.setOnClickListener {
            onRecipeClick(recipe)
        }
    }

    override fun getItemCount(): Int = recipes.size
}