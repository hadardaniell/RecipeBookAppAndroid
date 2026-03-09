package com.example.recipebookappandorid.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentProfileBinding
import com.example.recipebookappandorid.viewmodel.AuthViewModel
import com.example.recipebookappandorid.viewmodel.ProfileViewModel

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProfileBinding.bind(view)

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            findNavController().navigate(R.id.loginFragment)
        }

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.loadCurrentUser()
    }

    private fun observeViewModel() {
        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvName.text = user.name
                binding.tvEmail.text = user.email
            } else {
                binding.tvName.text = "No user found"
                binding.tvEmail.text = ""
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}