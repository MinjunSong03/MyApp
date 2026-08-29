package org.example.myapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.myapp.auth.model.OAuthProvider
import org.example.myapp.auth.repository.AuthRepository

class AuthViewModel(
    private val authRepository: AuthRepository
): ViewModel() {
    val authState = authRepository.authState

    // 오류 메세지 Toast
    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    // 닉네임 변경 성공 시
    private val _updateSuccessEvent = Channel<Unit>(Channel.BUFFERED)
    val updateSuccessEvent = _updateSuccessEvent.receiveAsFlow()

    // Concurrency 문제 해결
    private var authJob: Job? = null

    private fun executeAuthAction(action: suspend () -> Unit) {
        if (authJob?.isActive == true) return

        authJob = viewModelScope.launch {
            action()
        }
    }

    fun checkAutoLogin() {
        executeAuthAction {
            authRepository.checkAutoLogin()
        }
    }

    fun login(provider: OAuthProvider) {
        executeAuthAction {
            authRepository.login(provider)
                .onSuccess {
                    _toastEvent.send("로그인 되었습니다.")
                }
                .onFailure { error ->
                    _toastEvent.send(error.message ?: "로그인 처리에 실패했습니다.")
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
                    _toastEvent.send(error.message ?: "회원탈퇴 처리에 실패했습니다.")
                }
        }
    }

    fun updateNickname(nickname: String) {
        executeAuthAction {
            authRepository.updateNickname(nickname)
                .onSuccess {
                    _toastEvent.send("닉네임이 변경되었습니다.")
                    _updateSuccessEvent.send(Unit)
                }
                .onFailure { error ->
                    _toastEvent.send(error.message ?: "닉네임 변경에 실패했습니다.")
                }

        }
    }
}