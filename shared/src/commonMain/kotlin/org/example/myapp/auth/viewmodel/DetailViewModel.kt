package org.example.myapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.myapp.auth.repository.AuthRepository

class DetailViewModel(
    private val authRepository: AuthRepository
): ViewModel() {

    val authState = authRepository.authState

    // 닉네임 변경 성공 시
    private val _updateSuccessEvent = Channel<Unit>(Channel.BUFFERED)
    val updateSuccessEvent = _updateSuccessEvent.receiveAsFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
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