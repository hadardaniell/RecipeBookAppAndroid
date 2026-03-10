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

class AddRecipeFragment : Fragment(R.layout.fragment_add_recipe) {

    private var _binding: FragmentAddRecipeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddRecipeBinding.bind(view)

        binding.btnSaveRecipe.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            viewModel.addRecipe(title, description)
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