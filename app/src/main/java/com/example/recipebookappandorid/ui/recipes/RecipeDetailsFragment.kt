package com.example.recipebookappandorid.ui.recipe

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentRecipeDetailsBinding
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.viewmodel.RecipeViewModel

class RecipeDetailsFragment : Fragment(R.layout.fragment_recipe_details) {

    private var _binding: FragmentRecipeDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRecipeDetailsBinding.bind(view)

        val args = RecipeDetailsFragmentArgs.fromBundle(requireArguments())

        Glide.with(binding.ivRecipeImage)
            .load(args.imageUrl.ifBlank { null })
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(binding.ivRecipeImage)

        binding.tvRecipeTitle.text = args.title
        binding.tvRecipeAuthor.text = "By ${args.authorName}"
        binding.tvPrepTime.text = "Prep time: ${args.prepTime}"
        binding.tvDifficulty.text = "Difficulty: ${args.difficulty}"
        binding.tvCategory.text = "Category: ${args.category}"
        binding.tvIngredients.text = args.ingredients
        binding.tvSteps.text = args.steps
        binding.tvNotes.text = args.notes
        binding.btnImportRecipe.visibility = if (args.isRemote) View.VISIBLE else View.GONE

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnImportRecipe.setOnClickListener {
            viewModel.importRecipe(
                Recipe(
                    id = args.authorId,
                    title = args.title,
                    description = args.description,
                    imageUrl = args.imageUrl,
                    prepTime = args.prepTime,
                    difficulty = args.difficulty,
                    category = args.category,
                    ingredients = args.ingredients,
                    steps = args.steps,
                    notes = args.notes,
                    authorId = args.authorId,
                    authorName = args.authorName
                )
            )
        }

        viewModel.importSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Recipe imported", Toast.LENGTH_SHORT).show()
                binding.btnImportRecipe.isEnabled = false
                binding.btnImportRecipe.text = "Imported"
            }
        }

        viewModel.saveError.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
