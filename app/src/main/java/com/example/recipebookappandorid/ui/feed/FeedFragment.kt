package com.example.recipebookappandorid.ui.feed

import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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
        setupChips()
        observeSections()
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

    private fun setupChips() {
        binding.chipBreakfast.setOnClickListener { toggleCategory("Breakfast") }
        binding.chipPasta.setOnClickListener { toggleCategory("Pasta") }
        binding.chipHealthy.setOnClickListener { toggleCategory("Healthy") }
        binding.chipDessert.setOnClickListener { toggleCategory("Dessert") }
    }

    private fun toggleCategory(category: String) {
        selectedChip = if (selectedChip == category) null else category
        viewModel.setCategoryFilter(selectedChip)
        updateChipSelection()
    }

    private fun updateChipSelection() {
        binding.chipBreakfast.isChecked = selectedChip == "Breakfast"
        binding.chipPasta.isChecked = selectedChip == "Pasta"
        binding.chipHealthy.isChecked = selectedChip == "Healthy"
        binding.chipDessert.isChecked = selectedChip == "Dessert"
    }

    private fun observeSections() {
        viewModel.sections.observe(viewLifecycleOwner) { sections ->
            sectionsAdapter.submitList(sections)
        }
    }

    private fun openRecipe(recipe: Recipe) {
        val action = FeedFragmentDirections.actionFeedFragmentToRecipeDetailsFragment(
            title = recipe.title,
            authorName = recipe.authorName,
            prepTime = recipe.prepTime,
            difficulty = recipe.difficulty,
            category = recipe.category,
            ingredients = recipe.ingredients,
            steps = recipe.steps,
            notes = recipe.notes
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}