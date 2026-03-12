package com.example.recipebookappandorid.ui.recipes

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentAddRecipeBinding
import com.example.recipebookappandorid.viewmodel.RecipeViewModel
import android.widget.ArrayAdapter

class AddRecipeFragment : Fragment(R.layout.fragment_add_recipe) {

    private var _binding: FragmentAddRecipeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeViewModel by viewModels()

    private fun setupDropdowns() {
        val prepTimes = listOf(
            "10 min",
            "15 min",
            "20 min",
            "30 min",
            "45 min",
            "60 min+"
        )

        val difficulties = listOf(
            "Easy",
            "Medium",
            "Hard"
        )

        val categories = listOf(
            "Breakfast",
            "Lunch",
            "Dinner",
            "Dessert",
            "Salad",
            "Soup",
            "Pasta",
            "Main Dish",
            "Snack"
        )

        binding.etPrepTime.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, prepTimes)
        )

        binding.etDifficulty.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, difficulties)
        )

        binding.etCategory.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddRecipeBinding.bind(view)

        setupDropdowns()

        binding.btnSaveRecipe.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val prepTime = binding.etPrepTime.text.toString().trim()
            val difficulty = binding.etDifficulty.text.toString().trim()
            val category = binding.etCategory.text.toString().trim()
            val ingredients = binding.etIngredients.text.toString().trim()
            val steps = binding.etSteps.text.toString().trim()
            val notes = binding.etNotes.text.toString().trim()

            viewModel.addRecipe(
                title = title,
                description = description,
                prepTime = prepTime,
                difficulty = difficulty,
                category = category,
                ingredients = ingredients,
                steps = steps,
                notes = notes
            )
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.titleError.observe(viewLifecycleOwner) { error ->
            binding.titleInputLayout.error = error
        }

        viewModel.descriptionError.observe(viewLifecycleOwner) { error ->
            binding.descriptionInputLayout.error = error
        }

        viewModel.prepTimeError.observe(viewLifecycleOwner) { error ->
            binding.prepTimeInputLayout.error = error
        }

        viewModel.difficultyError.observe(viewLifecycleOwner) { error ->
            binding.difficultyInputLayout.error = error
        }

        viewModel.categoryError.observe(viewLifecycleOwner) { error ->
            binding.categoryInputLayout.error = error
        }

        viewModel.ingredientsError.observe(viewLifecycleOwner) { error ->
            binding.ingredientsInputLayout.error = error
        }

        viewModel.stepsError.observe(viewLifecycleOwner) { error ->
            binding.stepsInputLayout.error = error
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Recipe saved", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}