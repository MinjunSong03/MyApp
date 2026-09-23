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
import org.example.myapp.auth.network.UserResponse
import org.example.myapp.auth.repository.UserBlockRepository
import kotlin.collections.plus
import kotlin.coroutines.cancellation.CancellationException

data class BlockedUserUiState(
    val users: List<UserResponse> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLast: Boolean = false,
    val page: Int = 0
)

class BlockedUserViewModel(
    private val userBlockRepository: UserBlockRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(BlockedUserUiState())
    val uiState = _uiState.asStateFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private var feedJob: Job? = null

    fun loadMyBlockedUser(isRefresh: Boolean) {

        if (isRefresh) {
            feedJob?.cancel()
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    isLast = false
            )
            }
        } else {
            if (_uiState.value.isLast || feedJob?.isActive == true) return
            if (_uiState.value.users.isEmpty()) {
                _uiState.update { it.copy(isInitialLoading = true) }
            }
        }

        val targetPage = if (isRefresh) 0 else _uiState.value.page

        feedJob = viewModelScope.launch {
            try {
                userBlockRepository.getMyBlockedUser(targetPage)
                .onSuccess { slice ->
                    _uiState.update { current ->
                        val newUsers = if (isRefresh) slice.content else (current.users + slice.content).distinctBy { it.id }
                        current.copy(
                            users = newUsers,
                            page = targetPage + 1,
                            isLast = slice.last,
                            isInitialLoading = false,
                            isRefreshing = false
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val message = e.message ?: return@launch
                _toastEvent.send(message)
            } finally {
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }
    fun unblockUser(targetUserId: Long) {
        viewModelScope.launch {
            userBlockRepository.unblockUser(targetUserId)
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(users = current.users.filterNot { it.id == targetUserId })
                    }
                    _toastEvent.send("사용자를 차단 해제하였습니다.")
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }
}