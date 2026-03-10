package com.example.recipebookappandorid.ui.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.recipebookappandorid.R
import com.google.firebase.auth.FirebaseAuth

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.postDelayed({
            val user = FirebaseAuth.getInstance().currentUser

            if (user != null) {
                findNavController().navigate(R.id.mainContainerFragment)
            } else {
                findNavController().navigate(R.id.loginFragment)
            }
        }, 1000)
    }
}