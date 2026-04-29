package com.example.recipebookappandorid.ui.feed

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentFeedBinding
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.ui.feed.adapter.RecipeSectionsAdapter
import com.example.recipebookappandorid.viewmodel.FeedViewModel

class FeedFragment : Fragment(R.layout.fragment_feed) {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FeedViewModel by viewModels()
    private lateinit var sectionsAdapter: RecipeSectionsAdapter

    private var selectedChip: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentFeedBinding.bind(view)

        setupRecyclerView()
        setupSearch()
        observeSections()
        observeCategories()
        observeLoading()
        observeEmptyState()
    }

    private fun setupRecyclerView() {
        sectionsAdapter = RecipeSectionsAdapter { recipe ->
            openRecipe(recipe)
        }

        binding.rvFeedSections.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFeedSections.adapter = sectionsAdapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { editable ->
            viewModel.setSearchQuery(editable?.toString().orEmpty())
        }
    }

    private fun toggleCategory(category: String) {
        selectedChip = if (selectedChip == category) null else category
        viewModel.setCategoryFilter(selectedChip)
        renderCategoryChips(viewModel.categories.value.orEmpty())
    }

    private fun observeCategories() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            renderCategoryChips(categories)
        }
    }

    private fun observeSections() {
        viewModel.sections.observe(viewLifecycleOwner) { sections ->
            sectionsAdapter.submitList(sections)
        }
    }

    private fun observeLoading() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressFeed.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun observeEmptyState() {
        viewModel.emptyStateMessage.observe(viewLifecycleOwner) { message ->
            binding.tvEmptyState.text = message
            binding.tvEmptyState.visibility =
                if (message.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }

    private fun renderCategoryChips(categories: List<String>) {
        binding.chipGroupCategories.removeAllViews()

        categories.forEach { category ->
            val chip = Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                setChipDrawable(
                    com.google.android.material.chip.ChipDrawable.createFromAttributes(
                        context,
                        null,
                        0,
                        R.style.Widget_RecipeBook_FilterChip
                    )
                )
                text = category
                isCheckable = true
                isChecked = selectedChip == category
                chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.white)
                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (isChecked) R.color.white else R.color.navy_500
                    )
                )
                chipStrokeWidth = resources.displayMetrics.density
                chipStrokeColor = ContextCompat.getColorStateList(
                    requireContext(),
                    if (isChecked) R.color.navy_500 else R.color.navy_500
                )
                chipCornerRadius = 12f * resources.displayMetrics.density
                setCheckedIconVisible(false)
                rippleColor = ContextCompat.getColorStateList(requireContext(), android.R.color.transparent)
                ensureAccessibleTouchTarget(32)
                if (isChecked) {
                    chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.navy_500)
                }
                setOnClickListener { toggleCategory(category) }
            }
            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun openRecipe(recipe: Recipe) {
        viewModel.recordRecipeViewed(recipe)
        val action = FeedFragmentDirections.actionFeedFragmentToRecipeDetailsFragment(
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
            isRemote = recipe.authorId == "themealdb"
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
