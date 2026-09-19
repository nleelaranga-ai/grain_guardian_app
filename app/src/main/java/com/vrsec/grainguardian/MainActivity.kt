package com.vrsec.grainguardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.vrsec.grainguardian.ui.navigation.GrainNavigation
import com.vrsec.grainguardian.ui.theme.GrainGuardianTheme
import com.vrsec.grainguardian.viewmodel.GrainViewModel
import com.vrsec.grainguardian.viewmodel.GrainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as GrainGuardianApp
        val repository = app.repository

        setContent {
            GrainGuardianTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: GrainViewModel = viewModel(
                        factory = GrainViewModelFactory(repository)
                    )
                    GrainNavigation(navController = navController, viewModel = viewModel)
                }
            }
        }
    }
}
