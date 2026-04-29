package com.example.recipebookappandorid.ui.recipes

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
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
        val currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()
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
        if (!args.isRemote) {
            viewModel.markRecipeViewed(args.id)
        }

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
        binding.tvSharedBook.visibility = if (args.sharedBookName.isBlank()) View.GONE else View.VISIBLE
        binding.tvSharedBook.text = getString(R.string.recipe_shared_book_format, args.sharedBookName)
        renderIngredients(args.ingredients)
        binding.tvSteps.text = args.steps
        binding.tvNotes.text = args.notes

        val isMyRecipe = currentUserId == args.authorId
        val canImportToMyRecipes = !isMyRecipe && args.sharedBookId.isBlank()
        if (args.isRemote) {
            binding.btnImportRecipe.visibility = View.VISIBLE
            binding.layoutRecipeActions.visibility = View.GONE
            binding.btnEditRecipe.visibility = View.GONE
            binding.btnShareRecipe.visibility = View.GONE
        } else {
            binding.btnImportRecipe.visibility = if (canImportToMyRecipes) View.VISIBLE else View.GONE
            binding.layoutRecipeActions.visibility = View.GONE
            binding.btnShareRecipe.visibility = if (isMyRecipe) View.VISIBLE else View.GONE
            binding.btnEditRecipe.visibility = View.GONE

            if (args.sharedBookId.isBlank()) {
                binding.layoutRecipeActions.visibility = if (isMyRecipe) View.VISIBLE else View.GONE
                binding.btnEditRecipe.visibility = if (isMyRecipe) View.VISIBLE else View.GONE
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    val book = SharedRecipeBookRepository(requireContext()).getBookById(args.sharedBookId)
                    val currentRole = book?.roleFor(currentUserId)
                    val canEditRecipe = isMyRecipe ||
                        currentRole == SharedBookRole.OWNER ||
                        currentRole == SharedBookRole.EDITOR
                    if (_binding != null) {
                        binding.layoutRecipeActions.visibility = if (canEditRecipe) View.VISIBLE else View.GONE
                        binding.btnEditRecipe.visibility = if (canEditRecipe) View.VISIBLE else View.GONE
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

        binding.btnShareToBook.setOnClickListener {
            showShareToBookDialog()
        }

        binding.btnShareRecipe.setOnClickListener {
            showShareDialog(args.id)
        }

        viewModel.importSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Snackbar.make(binding.root, "Recipe imported to your recipe book", Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActions)
                    .show()
                binding.btnImportRecipe.isEnabled = false
                binding.btnImportRecipe.text = getString(R.string.imported)
            }
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                binding.tvSharedBook.visibility =
                    if (currentRecipe.sharedBookName.isBlank()) View.GONE else View.VISIBLE
                binding.tvSharedBook.text =
                    getString(R.string.recipe_shared_book_format, currentRecipe.sharedBookName)
                Snackbar.make(binding.root, "Done successfully", Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActions)
                    .show()
            }
        }

        viewModel.saveError.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.bottomActions)
                    .show()
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
                    Snackbar.make(binding.root, "Please enter an email address", Snackbar.LENGTH_SHORT)
                        .setAnchorView(binding.bottomActions)
                        .show()
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

    private fun showShareToBookDialog() {
        val firebaseUser = authRepository.getCurrentUser()
        if (firebaseUser == null) {
            Snackbar.make(binding.root, "You need to be logged in to share recipes", Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.bottomActions)
                .show()
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
                Snackbar.make(binding.root, "You do not have any shared books available", Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActions)
                    .show()
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
        viewModel.shareRecipeToBookAsCopy(
            recipe = currentRecipe,
            bookId = book.id,
            bookName = book.name,
            memberIds = book.memberIds,
            role = book.roleFor(currentUserId).orEmpty()
        )
    }
}
