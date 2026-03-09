package com.example.recipebookappandorid.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentEditProfileBinding
import com.example.recipebookappandorid.viewmodel.ProfileViewModel

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditProfileBinding.bind(view)

        observeViewModel()
        viewModel.loadCurrentUser()

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            viewModel.updateProfile(name)
        }
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.etName.setText(user.name)
                binding.tvEmail.text = user.email
            }
        }

        viewModel.nameError.observe(viewLifecycleOwner) { error ->
            binding.nameInputLayout.error = error
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}