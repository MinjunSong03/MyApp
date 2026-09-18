package org.example.myapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.myapp.auth.viewmodel.AppUiState
import org.example.myapp.auth.viewmodel.AppViewModel
import org.example.myapp.ui.LoginScreen
import org.example.myapp.ui.MainScreen
import org.example.myapp.ui.ProfileSetupScreen
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun App() {
    KoinContext {
        MaterialTheme {
            val viewModel: AppViewModel = koinViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            when (val state = uiState) {
                is AppUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (!state.message.isNullOrEmpty()) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                is AppUiState.Login -> {
                    LoginScreen()
                }
                is AppUiState.ProfileSetup -> {
                    ProfileSetupScreen(
                        onBack = { viewModel.cancelProfileSetup() }
                    )
                }
                is AppUiState.Main -> {
                    MainScreen()
                }
            }
        }
    }
}