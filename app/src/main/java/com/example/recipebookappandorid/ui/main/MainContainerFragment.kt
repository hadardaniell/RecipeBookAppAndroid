package com.example.recipebookappandorid.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentMainContainerBinding

class MainContainerFragment : Fragment(R.layout.fragment_main_container) {

    private var _binding: FragmentMainContainerBinding? = null
    private val binding get() = _binding!!
    private var updatingBottomNav = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMainContainerBinding.bind(view)

        val navHostFragment =
            childFragmentManager.findFragmentById(R.id.mainNavHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (updatingBottomNav) return@setOnItemSelectedListener true

            val destinationId = when (item.itemId) {
                R.id.feedFragment -> R.id.feedFragment
                R.id.addRecipeFragment -> R.id.addRecipeFragment
                R.id.profileFragment -> R.id.profileFragment
                else -> return@setOnItemSelectedListener false
            }

            val options = navOptions {
                if (item.itemId == R.id.addRecipeFragment) {
                    popUpTo(R.id.feedFragment) {
                        inclusive = false
                        saveState = false
                    }
                } else {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(R.id.feedFragment) {
                        saveState = true
                    }
                }
            }

            runCatching {
                navController.navigate(destinationId, null, options)
            }.isSuccess
        }

        binding.bottomNavigation.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.feedFragment &&
                navController.currentDestination?.id != R.id.feedFragment
            ) {
                navController.popBackStack(R.id.feedFragment, false)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val selectedItemId = when (destination.id) {
                R.id.profileFragment, R.id.editProfileFragment -> R.id.profileFragment
                R.id.addRecipeFragment -> R.id.addRecipeFragment
                else -> R.id.feedFragment
            }

            if (binding.bottomNavigation.selectedItemId != selectedItemId) {
                updatingBottomNav = true
                binding.bottomNavigation.menu.findItem(selectedItemId)?.isChecked = true
                updatingBottomNav = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
