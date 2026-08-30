package org.example.myapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.example.myapp.auth.repository.AuthRepository

class AppViewModel(
    private val authRepository: AuthRepository
): ViewModel() {
    val authState = authRepository.authState

    fun checkAutoLogin() {
        viewModelScope.launch {
            authRepository.checkAutoLogin()
        }
    }
}