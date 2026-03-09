package com.example.recipebookappandorid.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentRegisterBinding
import com.example.recipebookappandorid.viewmodel.AuthViewModel

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            viewModel.register(name, email, password, confirmPassword)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !isLoading
        }

        viewModel.nameError.observe(viewLifecycleOwner) { error ->
            binding.nameInputLayout.error = error
        }

        viewModel.emailError.observe(viewLifecycleOwner) { error ->
            binding.emailInputLayout.error = error
        }

        viewModel.passwordError.observe(viewLifecycleOwner) { error ->
            binding.passwordInputLayout.error = error
        }

        viewModel.confirmPasswordError.observe(viewLifecycleOwner) { error ->
            binding.confirmPasswordInputLayout.error = error
        }

        viewModel.registerError.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.registerSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.feedFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}