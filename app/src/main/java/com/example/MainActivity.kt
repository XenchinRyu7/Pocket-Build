package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.PocketBuildRepository
import com.example.ui.PocketBuildApp
import com.example.ui.PocketBuildViewModel
import com.example.ui.PocketBuildViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val database = AppDatabase.getDatabase(context.applicationContext)
            val repository = PocketBuildRepository(database)
            val factory = PocketBuildViewModelFactory(context.applicationContext, repository)
            val viewModel: PocketBuildViewModel = viewModel(factory = factory)
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            
            val darkTheme = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            
            MyApplicationTheme(darkTheme = darkTheme) {
                PocketBuildApp(viewModel = viewModel)
            }
        }
    }
}
