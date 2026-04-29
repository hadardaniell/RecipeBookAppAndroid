package com.example.recipebookappandorid.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentMyRecipesBinding
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.viewmodel.MyRecipesViewModel
import com.example.recipebookappandorid.viewmodel.RecipeViewModel

class MyRecipesFragment : Fragment(R.layout.fragment_my_recipes) {

    private var _binding: FragmentMyRecipesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyRecipesViewModel by viewModels()
    private val recipeViewModel: RecipeViewModel by viewModels()
    private lateinit var recipesAdapter: MyRecipesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyRecipesBinding.bind(view)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        recipesAdapter = MyRecipesAdapter(::openRecipe, ::confirmRemoveRecipe)
        binding.rvMyRecipes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyRecipes.adapter = recipesAdapter

        viewModel.myRecipes.observe(viewLifecycleOwner) { recipes ->
            recipesAdapter.submitList(recipes)
            binding.tvMyRecipesEmpty.visibility =
                if (recipes.isEmpty()) View.VISIBLE else View.GONE
        }

        recipeViewModel.deleteSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Snackbar.make(binding.root, "Recipe removed from your book", Snackbar.LENGTH_LONG).show()
            }
        }

        recipeViewModel.saveError.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
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
            sharedBookId = recipe.sharedBookId,
            sharedBookName = recipe.sharedBookName,
            sharedWithUserIds = recipe.sharedWithUserIds.toTypedArray(),
            sharedRole = recipe.sharedRole,
            isRemote = false
        )
        findNavController().navigate(action)
    }

    private fun confirmRemoveRecipe(recipe: Recipe) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove recipe?")
            .setMessage("This will remove the recipe from your personal recipe book.")
            .setPositiveButton("Remove") { _, _ ->
                recipeViewModel.deleteRecipe(recipe.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
