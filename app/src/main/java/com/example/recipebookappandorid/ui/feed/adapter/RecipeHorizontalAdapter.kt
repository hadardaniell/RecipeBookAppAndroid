package com.example.recipebookappandorid.ui.feed.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.ItemRecipeBinding
import com.example.recipebookappandorid.model.Recipe

class RecipeHorizontalAdapter(
    private val recipes: List<Recipe>
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
        holder.binding.tvDescription.text = recipe.prepTime
        holder.binding.tvAuthor.text = "By ${recipe.authorName}"

        Glide.with(holder.itemView.context)
            .load(recipe.imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(holder.binding.ivRecipe)
    }

    override fun getItemCount(): Int = recipes.size
}