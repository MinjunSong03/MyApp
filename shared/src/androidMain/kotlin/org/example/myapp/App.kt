package org.example.myapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import org.example.myapp.auth.viewmodel.*
import org.koin.compose.KoinContext
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.myapp.auth.model.AuthState
import org.example.myapp.ui.LoginScreen
import org.example.myapp.ui.MainScreen
import org.example.myapp.ui.ProfileSetupScreen
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun App() {
    KoinContext {
        MaterialTheme {
            val viewModel: AuthViewModel = koinViewModel()
            val authState by viewModel.authState.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.checkAutoLogin()
            }

            when (val state = authState) {
                is AuthState.Initial, is AuthState.Loading -> {
                    val message = (state as? AuthState.Loading)?.message ?: "로딩 중..."
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color.Black)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = message,
                            fontSize = 16.sp
                        )
                    }
                }
                is AuthState.Unauthenticated -> {
                    LoginScreen()
                }
                is AuthState.Authenticated -> {
                    if (state.isNewUser) {
                        ProfileSetupScreen()
                    } else {
                        MainScreen()
                    }
                }
            }
        }
    }
}