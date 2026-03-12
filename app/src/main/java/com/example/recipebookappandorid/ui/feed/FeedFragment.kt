package com.example.recipebookappandorid.ui.feed

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentFeedBinding
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.ui.feed.adapter.RecipeHorizontalAdapter

class FeedFragment : Fragment(R.layout.fragment_feed) {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFeedBinding.bind(view)

        val recipes = listOf(
            Recipe(
                title = "Creamy Mushroom Pasta",
                imageUrl = "https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9",
                prepTime = "20 min",
                authorName = "Asaf"
            ),
            Recipe(
                title = "Shakshuka",
                imageUrl = "https://images.unsplash.com/photo-1547592180-85f173990554",
                prepTime = "15 min",
                authorName = "Hadar"
            )
        )

        binding.rvPopularRecipes.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.rvPopularRecipes.adapter = RecipeHorizontalAdapter(recipes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}