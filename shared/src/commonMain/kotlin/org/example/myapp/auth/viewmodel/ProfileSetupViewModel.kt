package org.example.myapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.myapp.auth.repository.AuthRepository

class ProfileSetupViewModel(
    private val authRepository: AuthRepository
): ViewModel() {
    val authState = authRepository.authState

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            authRepository.updateNickname(nickname)
                .onSuccess {
                    _toastEvent.send("닉네임이 변경되었습니다.")
                }
                .onFailure { error ->
                    _toastEvent.send(error.message ?: "닉네임 변경에 실패했습니다.")
                }
        }
    }
}