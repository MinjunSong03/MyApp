package org.example.myapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.myapp.auth.model.AuthState
import org.example.myapp.auth.model.OAuthProvider
import org.example.myapp.auth.repository.AuthRepository

data class MyInfoUiState(
    val nickname: String = "",
    val userId: String = "-",
    val isLoading: Boolean = false
)

class MyInfoViewModel(
    private val authRepository: AuthRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(MyInfoUiState())
    val uiState = _uiState.asStateFlow()
    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private var authJob: Job? = null

    private fun executeAuthAction(action: suspend () -> Unit) {
        if (authJob?.isActive == true) return
        authJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                action()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    init {
        viewModelScope.launch {
            authRepository.authState.collect { auth ->
                if (auth is AuthState.Authenticated) {
                    _uiState.update {
                        it.copy(
                            nickname = auth.session.nickname ?: "사용자",
                            userId = auth.session.userId?.toString() ?: "-"
                        )
                    }
                }
            }
        }
    }

    fun logout(provider: OAuthProvider?) {
        executeAuthAction {
            authRepository.logout(provider)
                .onSuccess {
                    _toastEvent.send("로그아웃 처리되었습니다.")
                }
                .onFailure { _toastEvent.send("로그아웃 처리되었습니다.") }
        }
    }

    fun unlink(provider: OAuthProvider) {
        executeAuthAction {
            authRepository.unlink(provider)
                .onSuccess {
                    _toastEvent.send("회원탈퇴 처리 완료되었습니다.")
                }
                .onFailure { error ->
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }
}