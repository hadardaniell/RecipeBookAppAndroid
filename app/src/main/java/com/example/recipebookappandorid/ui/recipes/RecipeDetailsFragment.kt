package com.example.recipebookappandorid.ui.recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.app.AlertDialog
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentRecipeDetailsBinding
import com.example.recipebookappandorid.databinding.ItemIngredientDisplayBinding
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.model.SharedBookRole
import com.example.recipebookappandorid.model.SharedRecipeBook
import com.example.recipebookappandorid.repository.AuthRepository
import com.example.recipebookappandorid.repository.SharedRecipeBookRepository
import com.example.recipebookappandorid.util.IngredientsCodec
import com.example.recipebookappandorid.viewmodel.RecipeViewModel
import kotlinx.coroutines.launch

class RecipeDetailsFragment : Fragment(R.layout.fragment_recipe_details) {

    private var _binding: FragmentRecipeDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeViewModel by viewModels()
    private val authRepository = AuthRepository()
    private lateinit var currentRecipe: Recipe

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRecipeDetailsBinding.bind(view)

        val args = RecipeDetailsFragmentArgs.fromBundle(requireArguments())
        currentRecipe = Recipe(
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
            authorName = args.authorName,
            sharedBookId = args.sharedBookId,
            sharedBookName = args.sharedBookName,
            sharedWithUserIds = args.sharedWithUserIds.toList(),
            sharedRole = args.sharedRole
        )

        Glide.with(binding.ivRecipeImage)
            .load(args.imageUrl.ifBlank { null })
            .placeholder(R.drawable.ic_recipe_placeholder)
            .error(R.drawable.ic_recipe_placeholder)
            .into(binding.ivRecipeImage)

        binding.tvRecipeTitle.text = args.title
        binding.tvRecipeAuthor.text = "By ${args.authorName}"
        binding.tvPrepTime.text = "Prep time: ${args.prepTime}"
        binding.tvDifficulty.text = "Difficulty: ${args.difficulty}"
        binding.tvCategory.text = "Category: ${args.category}"
        binding.tvSharedBook.visibility = if (args.sharedBookName.isBlank()) View.GONE else View.VISIBLE
        binding.tvSharedBook.text = "Shared book: ${args.sharedBookName}"
        renderIngredients(args.ingredients)
        binding.tvSteps.text = args.steps
        binding.tvNotes.text = args.notes
        binding.btnImportRecipe.visibility = if (args.isRemote) View.VISIBLE else View.GONE
        val currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()
        binding.layoutRecipeActions.visibility = View.GONE

        if (!args.isRemote) {
            if (args.sharedBookId.isBlank()) {
                binding.layoutRecipeActions.visibility =
                    if (args.authorId == currentUserId) View.VISIBLE else View.GONE
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    val book = SharedRecipeBookRepository(requireContext()).getBookById(args.sharedBookId)
                    val currentRole = book?.roleFor(currentUserId)
                    val canEditRecipe = args.authorId == currentUserId ||
                        currentRole == SharedBookRole.OWNER ||
                        currentRole == SharedBookRole.EDITOR
                    if (_binding != null) {
                        binding.layoutRecipeActions.visibility =
                            if (canEditRecipe) View.VISIBLE else View.GONE
                    }
                }
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
                    sharedBookId = args.sharedBookId,
                    sharedBookName = args.sharedBookName,
                    sharedWithUserIds = args.sharedWithUserIds.toList(),
                    sharedRole = args.sharedRole,
                    authorId = args.authorId,
                    authorName = args.authorName
                )
            )
        }

        binding.btnEditRecipe.setOnClickListener {
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
                sharedBookId = args.sharedBookId,
                sharedBookName = args.sharedBookName,
                isEditMode = true
            )
            findNavController().navigate(action)
        }

        binding.btnDeleteRecipe.setOnClickListener {
            viewModel.deleteRecipe(args.id)
        }

        binding.btnShareToBook.setOnClickListener {
            showShareToBookDialog()
        }

        viewModel.importSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Recipe imported", Toast.LENGTH_SHORT).show()
                binding.btnImportRecipe.isEnabled = false
                binding.btnImportRecipe.text = "Imported"
            }
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                binding.tvSharedBook.visibility =
                    if (currentRecipe.sharedBookName.isBlank()) View.GONE else View.VISIBLE
                binding.tvSharedBook.text = "Shared book: ${currentRecipe.sharedBookName}"
                Toast.makeText(requireContext(), "Recipe saved", Toast.LENGTH_SHORT).show()
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

    private fun showShareToBookDialog() {
        val firebaseUser = authRepository.getCurrentUser()
        if (firebaseUser == null) {
            Toast.makeText(requireContext(), "You must be logged in", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val books = SharedRecipeBookRepository(requireContext())
                .getBooksForUser(firebaseUser.uid, firebaseUser.email.orEmpty())
                .filter { book ->
                    val role = book.roleFor(firebaseUser.uid)
                    role == SharedBookRole.OWNER || role == SharedBookRole.EDITOR
                }

            if (books.isEmpty()) {
                Toast.makeText(requireContext(), "No shared books available", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val labels = books.map(SharedRecipeBook::name).toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Share To Book")
                .setItems(labels) { _, which ->
                    shareRecipeToBook(books[which], firebaseUser.uid)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun shareRecipeToBook(book: SharedRecipeBook, currentUserId: String) {
        currentRecipe = currentRecipe.copy(
            sharedBookId = book.id,
            sharedBookName = book.name,
            sharedWithUserIds = book.memberIds,
            sharedRole = book.roleFor(currentUserId).orEmpty()
        )

        viewModel.updateRecipe(
            recipeId = currentRecipe.id,
            title = currentRecipe.title,
            description = currentRecipe.description,
            imageUrl = currentRecipe.imageUrl,
            prepTime = currentRecipe.prepTime,
            difficulty = currentRecipe.difficulty,
            category = currentRecipe.category,
            ingredients = IngredientsCodec.decode(currentRecipe.ingredients),
            steps = currentRecipe.steps,
            notes = currentRecipe.notes,
            sharedBookId = currentRecipe.sharedBookId,
            sharedBookName = currentRecipe.sharedBookName
        )
    }
}
