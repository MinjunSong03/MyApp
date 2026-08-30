package org.example.myapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.myapp.auth.model.OAuthProvider

import org.example.myapp.auth.repository.AuthRepository

class LoginViewModel(
    private val authRepository: AuthRepository
): ViewModel() {
    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    fun login(provider: OAuthProvider) {
        viewModelScope.launch {
            authRepository.login(provider)
                .onSuccess {
                    _toastEvent.send("로그인 되었습니다.")
                }
                .onFailure { error ->
                    _toastEvent.send(error.message ?: "로그인 처리에 실패했습니다.")
                }
        }
    }
}