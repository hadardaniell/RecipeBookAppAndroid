package com.example.recipebookappandorid.ui.recipe

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentRecipeDetailsBinding

class RecipeDetailsFragment : Fragment(R.layout.fragment_recipe_details) {

    private var _binding: FragmentRecipeDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRecipeDetailsBinding.bind(view)

        val args = RecipeDetailsFragmentArgs.fromBundle(requireArguments())

        binding.tvRecipeTitle.text = args.title
        binding.tvRecipeAuthor.text = "By ${args.authorName}"
        binding.tvPrepTime.text = "Prep time: ${args.prepTime}"
        binding.tvDifficulty.text = "Difficulty: ${args.difficulty}"
        binding.tvCategory.text = "Category: ${args.category}"
        binding.tvIngredients.text = args.ingredients
        binding.tvSteps.text = args.steps
        binding.tvNotes.text = args.notes

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}