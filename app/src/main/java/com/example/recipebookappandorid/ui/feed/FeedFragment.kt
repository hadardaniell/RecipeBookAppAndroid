package com.example.recipebookappandorid.ui.feed

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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
                id = "1",
                title = "Creamy Mushroom Pasta",
                description = "Rich and creamy pasta with mushrooms",
                prepTime = "20 min",
                difficulty = "Easy",
                category = "Pasta",
                ingredients = "Pasta, mushrooms, cream, parmesan",
                steps = "1. Boil pasta\n2. Cook mushrooms\n3. Add cream\n4. Mix together",
                notes = "Best served hot",
                authorName = "Asaf"
            ),
            Recipe(
                id = "2",
                title = "Shakshuka",
                description = "Classic shakshuka with eggs and tomato sauce",
                prepTime = "15 min",
                difficulty = "Easy",
                category = "Breakfast",
                ingredients = "Eggs, tomatoes, onion, garlic, paprika",
                steps = "1. Fry onion\n2. Add tomatoes\n3. Add eggs\n4. Cover and cook",
                notes = "Serve with bread",
                authorName = "Hadar"
            )
        )

        binding.rvPopularRecipes.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.rvPopularRecipes.adapter = RecipeHorizontalAdapter(recipes) { recipe ->

            val bundle = Bundle().apply {
                putString("title", recipe.title)
                putString("authorName", recipe.authorName)
                putString("prepTime", recipe.prepTime)
                putString("difficulty", recipe.difficulty)
                putString("category", recipe.category)
                putString("ingredients", recipe.ingredients)
                putString("steps", recipe.steps)
                putString("notes", recipe.notes)
            }

            findNavController().navigate(R.id.recipeDetailsFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}