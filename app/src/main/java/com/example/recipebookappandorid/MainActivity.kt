package com.example.recipebookappandorid

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContentView(R.layout.activity_main)
        showPreviousCrashIfNeeded()
    }

    private fun showPreviousCrashIfNeeded() {
        val prefs = getSharedPreferences(RecipeBookApplication.CRASH_PREFS, MODE_PRIVATE)
        val crash = prefs.getString(RecipeBookApplication.KEY_LAST_CRASH, null).orEmpty()
        if (crash.isBlank()) return

        prefs.edit().remove(RecipeBookApplication.KEY_LAST_CRASH).apply()

        AlertDialog.Builder(this)
            .setTitle("Last crash details")
            .setMessage(crash.take(3000))
            .setPositiveButton("OK", null)
            .show()
    }
}
