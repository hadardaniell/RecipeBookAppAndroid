package com.example.recipebookappandorid

import android.app.Application
import java.io.PrintWriter
import java.io.StringWriter

class RecipeBookApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveLastCrash(throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveLastCrash(throwable: Throwable) {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        getSharedPreferences(CRASH_PREFS, MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_CRASH, writer.toString())
            .apply()
    }

    companion object {
        const val CRASH_PREFS = "crash_debug"
        const val KEY_LAST_CRASH = "last_crash"
    }
}
