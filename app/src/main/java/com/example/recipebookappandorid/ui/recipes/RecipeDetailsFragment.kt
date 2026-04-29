package com.example.recipebookappandorid.ui.recipes

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentRecipeDetailsBinding
import com.example.recipebookappandorid.databinding.ItemIngredientDisplayBinding
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.repository.AuthRepository
import com.example.recipebookappandorid.util.IngredientsCodec
import com.example.recipebookappandorid.viewmodel.RecipeViewModel

class RecipeDetailsFragment : Fragment(R.layout.fragment_recipe_details) {

    private var _binding: FragmentRecipeDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeViewModel by viewModels()
    private val authRepository = AuthRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRecipeDetailsBinding.bind(view)

        val args = RecipeDetailsFragmentArgs.fromBundle(requireArguments())
        val currentUserId = authRepository.getCurrentUser()?.uid

        Glide.with(binding.ivRecipeImage)
            .load(args.imageUrl.ifBlank { null })
            .placeholder(R.drawable.ic_recipe_placeholder)
            .error(R.drawable.ic_recipe_placeholder)
            .into(binding.ivRecipeImage)

        binding.tvRecipeTitle.text = args.title
        binding.tvRecipeAuthor.text = getString(R.string.recipe_author_format, args.authorName)
        binding.tvPrepTime.text = getString(R.string.recipe_prep_time_format, args.prepTime)
        binding.tvDifficulty.text = getString(R.string.recipe_difficulty_format, args.difficulty)
        binding.tvCategory.text = getString(R.string.recipe_category_format, args.category)
        renderIngredients(args.ingredients)
        binding.tvSteps.text = args.steps
        binding.tvNotes.text = args.notes

        // Logic for buttons visibility based on ownership and source
        val isMyRecipe = currentUserId == args.authorId
        val isFromApi = args.isRemote

        when {
            isFromApi -> {
                binding.btnImportRecipe.visibility = View.VISIBLE
                binding.layoutRecipeActions.visibility = View.GONE
                binding.btnShareRecipe.visibility = View.GONE
            }
            isMyRecipe -> {
                // It's my own recipe, I can edit, delete, or share it
                binding.btnImportRecipe.visibility = View.GONE
                binding.layoutRecipeActions.visibility = View.VISIBLE
                binding.btnDeleteRecipe.visibility = View.VISIBLE
                binding.btnShareRecipe.visibility = View.VISIBLE
            }
            else -> {
                // It's a recipe shared by someone else
                binding.btnImportRecipe.visibility = View.GONE
                binding.layoutRecipeActions.visibility = View.VISIBLE
                // I can edit it (saving a copy for myself), but I cannot delete their original post
                binding.btnDeleteRecipe.visibility = View.GONE
                binding.btnShareRecipe.visibility = View.GONE
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnImportRecipe.setOnClickListener {
            viewModel.importRecipe(
                Recipe(
                    id = args.id,
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

        binding.btnEditRecipe.setOnClickListener {
            // When editing someone else's recipe, we import it as our own upon saving
            val action = RecipeDetailsFragmentDirections.actionRecipeDetailsFragmentToAddRecipeFragment(
                recipeId = args.id,
                title = args.title,
                description = args.description,
                imageUrl = args.imageUrl,
                prepTime = args.prepTime,
                difficulty = args.difficulty,
                category = args.category,
                ingredients = args.ingredients,
                steps = args.steps,
                notes = args.notes,
                isEditMode = true 
            )
            findNavController().navigate(action)
        }

        binding.btnDeleteRecipe.setOnClickListener {
            viewModel.deleteRecipe(args.id)
        }

        binding.btnShareRecipe.setOnClickListener {
            showShareDialog(args.id)
        }

        viewModel.importSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Recipe imported", Toast.LENGTH_SHORT).show()
                binding.btnImportRecipe.isEnabled = false
                binding.btnImportRecipe.text = getString(R.string.imported)
            }
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Action completed successfully", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.saveError.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.deleteSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Recipe deleted", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun showShareDialog(recipeId: String) {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            hint = "Enter friend's email"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Share Recipe")
            .setMessage("Who do you want to share this recipe with?")
            .setView(input)
            .setPositiveButton("Share") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty()) {
                    viewModel.shareRecipe(recipeId, email)
                } else {
                    Toast.makeText(requireContext(), "Email cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderIngredients(ingredientsValue: String) {
        binding.layoutIngredients.removeAllViews()

        val items = IngredientsCodec.decode(ingredientsValue)
        if (items.isEmpty()) {
            val fallback = ItemIngredientDisplayBinding.inflate(
                LayoutInflater.from(requireContext()),
                binding.layoutIngredients,
                false
            )
            fallback.tvIngredientQuantity.text = ""
            fallback.tvIngredientQuantity.visibility = View.GONE
            fallback.tvIngredientName.text = IngredientsCodec.toDisplayText(ingredientsValue)
            binding.layoutIngredients.addView(fallback.root)
            return
        }

        items.forEach { ingredient ->
            val itemBinding = ItemIngredientDisplayBinding.inflate(
                LayoutInflater.from(requireContext()),
                binding.layoutIngredients,
                false
            )

            val quantityText = listOf(ingredient.quantity, ingredient.unit)
                .filter { it.isNotBlank() }
                .joinToString(" ")

            itemBinding.tvIngredientQuantity.text = quantityText
            itemBinding.tvIngredientQuantity.visibility =
                if (quantityText.isBlank()) View.GONE else View.VISIBLE
            itemBinding.tvIngredientName.text = ingredient.name

            binding.layoutIngredients.addView(itemBinding.root)
        }
    }
}
