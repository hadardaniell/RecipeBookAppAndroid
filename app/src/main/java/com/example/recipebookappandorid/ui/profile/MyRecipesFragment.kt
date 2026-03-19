package com.example.recipebookappandorid.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentMyRecipesBinding
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.viewmodel.MyRecipesViewModel

class MyRecipesFragment : Fragment(R.layout.fragment_my_recipes) {

    private var _binding: FragmentMyRecipesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyRecipesViewModel by viewModels()
    private lateinit var recipesAdapter: MyRecipesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyRecipesBinding.bind(view)

        recipesAdapter = MyRecipesAdapter(::openRecipe)
        binding.rvMyRecipes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyRecipes.adapter = recipesAdapter

        viewModel.myRecipes.observe(viewLifecycleOwner) { recipes ->
            recipesAdapter.submitList(recipes)
            binding.tvMyRecipesEmpty.visibility =
                if (recipes.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun openRecipe(recipe: Recipe) {
        val action = MyRecipesFragmentDirections.actionMyRecipesFragmentToRecipeDetailsFragment(
            id = recipe.id,
            description = recipe.description,
            imageUrl = recipe.imageUrl,
            title = recipe.title,
            authorId = recipe.authorId,
            authorName = recipe.authorName,
            prepTime = recipe.prepTime,
            difficulty = recipe.difficulty,
            category = recipe.category,
            ingredients = recipe.ingredients,
            steps = recipe.steps,
            notes = recipe.notes,
            isRemote = false
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
