package com.example.recipebookappandorid.ui.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.recipebookappandorid.R
import com.google.firebase.auth.FirebaseAuth

class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val handler = Handler(Looper.getMainLooper())

    private val navigateRunnable = Runnable {
        if (!isAdded) return@Runnable

        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.splashFragment) return@Runnable

        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            navController.navigate(R.id.action_splashFragment_to_mainContainerFragment)
        } else {
            navController.navigate(R.id.action_splashFragment_to_loginFragment)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handler.postDelayed(navigateRunnable, 1000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(navigateRunnable)
    }
}