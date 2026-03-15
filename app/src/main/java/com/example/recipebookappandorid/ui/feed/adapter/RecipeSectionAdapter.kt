package com.example.recipebookappandorid.ui.feed.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebookappandorid.databinding.ItemRecipeSectionBinding
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.model.RecipeSection

class RecipeSectionsAdapter(
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeSectionsAdapter.SectionViewHolder>() {

    private val sections = mutableListOf<RecipeSection>()

    inner class SectionViewHolder(val binding: ItemRecipeSectionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemRecipeSectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val section = sections[position]

        holder.binding.tvSectionTitle.text = section.title

        val adapter = RecipeHorizontalAdapter(onRecipeClick)
        holder.binding.rvSectionRecipes.layoutManager =
            LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        holder.binding.rvSectionRecipes.adapter = adapter
        adapter.submitList(section.recipes)
    }

    override fun getItemCount(): Int = sections.size

    fun submitList(newSections: List<RecipeSection>) {
        sections.clear()
        sections.addAll(newSections)
        notifyDataSetChanged()
    }
}