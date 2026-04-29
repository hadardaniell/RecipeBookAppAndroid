package com.example.recipebookappandorid.ui.recipes

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.example.recipebookappandorid.databinding.ItemIngredientInputBinding
import com.example.recipebookappandorid.model.IngredientItem
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentAddRecipeBinding
import com.example.recipebookappandorid.util.IngredientsCodec
import com.example.recipebookappandorid.viewmodel.RecipeViewModel
import android.widget.ArrayAdapter

class AddRecipeFragment : Fragment(R.layout.fragment_add_recipe) {

    private var _binding: FragmentAddRecipeBinding? = null
    private val binding get() = _binding!!
    private lateinit var navArgs: AddRecipeFragmentArgs

    private val viewModel: RecipeViewModel by viewModels()
    private val ingredientRows = mutableListOf<ItemIngredientInputBinding>()
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val currentBinding = _binding ?: return@registerForActivityResult
        uri?.let {
            selectedImageUri = it
            renderRecipeImage(localUri = it, remoteUrl = null, bindingOverride = currentBinding)
        }
    }

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
        navArgs = AddRecipeFragmentArgs.fromBundle(requireArguments())

        setupDropdowns()
        renderRecipeImage(localUri = null, remoteUrl = navArgs.imageUrl)
        binding.btnBack.setOnClickListener {
            navigateBack(navArgs)
        }
        binding.btnSelectRecipeImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        binding.btnAddIngredient.setOnClickListener {
            addIngredientRow()
        }
        populateForEdit(navArgs)
        if (ingredientRows.isEmpty()) {
            addIngredientRow()
        }

        binding.btnSaveRecipe.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val prepTime = binding.etPrepTime.text.toString().trim()
            val difficulty = binding.etDifficulty.text.toString().trim()
            val category = binding.etCategory.text.toString().trim()
            val ingredients = collectIngredients()
            val steps = binding.etSteps.text.toString().trim()
            val notes = binding.etNotes.text.toString().trim()

            if (navArgs.isEditMode) {
                viewModel.updateRecipe(
                    recipeId = navArgs.recipeId,
                    title = title,
                    description = description,
                    imageUrl = navArgs.imageUrl,
                    prepTime = prepTime,
                    difficulty = difficulty,
                    category = category,
                    ingredients = ingredients,
                    steps = steps,
                    notes = notes,
                    imageUri = selectedImageUri,
                    sharedBookId = navArgs.sharedBookId,
                    sharedBookName = navArgs.sharedBookName
                )
            } else {
                viewModel.addRecipe(
                    title = title,
                    description = description,
                    prepTime = prepTime,
                    difficulty = difficulty,
                    category = category,
                    ingredients = ingredients,
                    steps = steps,
                    notes = notes,
                    imageUri = selectedImageUri,
                    sharedBookId = navArgs.sharedBookId,
                    sharedBookName = navArgs.sharedBookName
                )
            }
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
            binding.tvIngredientsError.text = error
            binding.tvIngredientsError.visibility = if (error.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        viewModel.stepsError.observe(viewLifecycleOwner) { error ->
            binding.stepsInputLayout.error = error
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Snackbar.make(binding.root, "Recipe saved", Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.btnSaveRecipe)
                    .show()
            }
        }

        viewModel.saveError.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.btnSaveRecipe)
                    .show()
            }
        }

        viewModel.isSaving.observe(viewLifecycleOwner) { isSaving ->
            binding.btnSaveRecipe.isEnabled = !isSaving
            binding.btnSelectRecipeImage.isEnabled = !isSaving
            binding.progressSaveRecipe.visibility = if (isSaving) View.VISIBLE else View.GONE
        }

        viewModel.savedRecipe.observe(viewLifecycleOwner) { recipe ->
            if (recipe != null) {
                viewModel.onRecipeNavigationHandled()
                if (!navArgs.isEditMode && navArgs.sharedBookId.isNotBlank()) {
                    val action =
                        AddRecipeFragmentDirections.actionAddRecipeFragmentToSharedBookDetailsFragment(
                            bookId = navArgs.sharedBookId,
                            bookName = navArgs.sharedBookName
                        )
                    findNavController().navigate(action)
                } else {
                    val action = AddRecipeFragmentDirections.actionAddRecipeFragmentToRecipeDetailsFragment(
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
            }
        }
    }

    private fun navigateBack(args: AddRecipeFragmentArgs) {
        if (!args.isEditMode && args.sharedBookId.isNotBlank()) {
            val action =
                AddRecipeFragmentDirections.actionAddRecipeFragmentToSharedBookDetailsFragment(
                    bookId = args.sharedBookId,
                    bookName = args.sharedBookName
                )
            findNavController().navigate(action)
        } else {
            findNavController().navigateUp()
        }
    }

    private fun populateForEdit(args: AddRecipeFragmentArgs) {
        if (!args.isEditMode) return

        binding.etTitle.setText(args.title)
        binding.etDescription.setText(args.description)
        binding.etPrepTime.setText(args.prepTime, false)
        binding.etDifficulty.setText(args.difficulty, false)
        binding.etCategory.setText(args.category, false)
        IngredientsCodec.decode(args.ingredients).forEach { ingredient ->
            addIngredientRow(ingredient)
        }
        binding.etSteps.setText(args.steps)
        binding.etNotes.setText(args.notes)
        binding.btnSaveRecipe.text = "Save Changes"
    }

    private fun renderRecipeImage(
        localUri: Uri?,
        remoteUrl: String?,
        bindingOverride: FragmentAddRecipeBinding? = _binding
    ) {
        val currentBinding = bindingOverride ?: return
        Glide.with(this)
            .load(localUri ?: remoteUrl?.ifBlank { null })
            .placeholder(R.drawable.ic_recipe_placeholder)
            .error(R.drawable.ic_recipe_placeholder)
            .centerCrop()
            .into(currentBinding.ivRecipePreview)
    }

    private fun addIngredientRow(ingredient: IngredientItem = IngredientItem()) {
        val rowBinding = ItemIngredientInputBinding.inflate(
            LayoutInflater.from(requireContext()),
            binding.ingredientsContainer,
            false
        )

        rowBinding.etIngredientName.setText(ingredient.name)
        rowBinding.etIngredientQuantity.setText(ingredient.quantity)
        rowBinding.etIngredientUnit.setText(ingredient.unit)
        rowBinding.btnRemoveIngredient.setOnClickListener {
            if (ingredientRows.size == 1) {
                rowBinding.etIngredientName.text = null
                rowBinding.etIngredientQuantity.text = null
                rowBinding.etIngredientUnit.text = null
            } else {
                binding.ingredientsContainer.removeView(rowBinding.root)
                ingredientRows.remove(rowBinding)
            }
        }

        ingredientRows.add(rowBinding)
        binding.ingredientsContainer.addView(rowBinding.root)
    }

    private fun collectIngredients(): List<IngredientItem> {
        return ingredientRows.map {
            IngredientsCodec.fromNameQuantityUnit(
                name = it.etIngredientName.text?.toString().orEmpty(),
                quantity = it.etIngredientQuantity.text?.toString().orEmpty(),
                unit = it.etIngredientUnit.text?.toString().orEmpty()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ingredientRows.clear()
        _binding = null
    }
}
