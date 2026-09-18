package org.example.myapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.example.myapp.auth.model.AuthState
import org.example.myapp.auth.repository.AuthRepository

sealed interface AppUiState {
    data class Loading(val message: String? = null) : AppUiState
    data object Login : AppUiState
    data object ProfileSetup : AppUiState
    data object Main : AppUiState
}

class AppViewModel(
    private val authRepository: AuthRepository
): ViewModel() {
    val uiState: StateFlow<AppUiState> = authRepository.authState
        .map { state ->
            when (state) {
                is AuthState.Initial -> AppUiState.Loading()
                is AuthState.Loading -> AppUiState.Loading(state.message)
                is AuthState.Unauthenticated -> AppUiState.Login
                is AuthState.Authenticated -> {
                    if (state.isNewUser) AppUiState.ProfileSetup
                    else AppUiState.Main
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppUiState.Loading()
        )

    init {
        checkAutoLogin()
    }
    fun checkAutoLogin() {
        viewModelScope.launch {
            authRepository.checkAutoLogin()
        }
    }

    fun cancelProfileSetup() {
        viewModelScope.launch {
            authRepository.logout(null)
        }
    }
}