package org.example.myapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.myapp.auth.network.BlockedUserResponse
import org.example.myapp.auth.repository.UserBlockRepository
import kotlin.coroutines.cancellation.CancellationException

sealed class ManageMyUiState {
    object Loading: ManageMyUiState()
    data class Success(val users: List<BlockedUserResponse>, val isLast: Boolean): ManageMyUiState()
}

class ManageMyViewModel(
    private val userBlockRepository: UserBlockRepository
): ViewModel() {
    private val _uiState = MutableStateFlow<ManageMyUiState>(ManageMyUiState.Loading)
    val uiState: StateFlow<ManageMyUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private var currentPage = 0
    private var isLastPage = false
    private val currentUserList = mutableListOf<BlockedUserResponse>()

    private var feedJob: Job? = null

    fun loadMyBlockedUser(isRefresh: Boolean = false) {

        if (isRefresh) {
            feedJob?.cancel()
            _isRefreshing.value = true
            currentPage = 0
            isLastPage = false
            currentUserList.clear()
            _uiState.value = ManageMyUiState.Loading
        } else {
            if (isLastPage || feedJob?.isActive == true) return
        }

        feedJob = viewModelScope.launch {
            try {
                userBlockRepository.getMyBlockedUser(currentPage)
                .onSuccess { slice ->
                    if (isRefresh) currentUserList.clear()
                    currentUserList.addAll(slice.content)
                    isLastPage = slice.last
                    currentPage++
                    _uiState.value = ManageMyUiState.Success(currentUserList.toList(), isLastPage)
                }
                    .onFailure { error ->
                        if (error is CancellationException) return@onFailure
                        if (currentUserList.isEmpty()) {
                            _uiState.value = ManageMyUiState.Success(emptyList(), isLast = true)
                        } else {
                            _uiState.value = ManageMyUiState.Success(currentUserList.toList(), isLastPage)
                        }
                        val message = error.message ?: return@onFailure
                        _toastEvent.send(message)
                    }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = ManageMyUiState.Success(currentUserList.toList(), isLastPage)
                val message = e.message ?: return@launch
                _toastEvent.send(message)
            } finally {
                if (isRefresh) {
                    _isRefreshing.value = false
                }
            }
        }
    }
    fun unblockUser(targetUserId: Long) {
        viewModelScope.launch {
            userBlockRepository.unblockUser(targetUserId)
                .onSuccess {
                    currentUserList.removeAll { it.id == targetUserId }
                    _uiState.value = ManageMyUiState.Success(currentUserList.toList(), isLastPage)
                    _toastEvent.send("사용자를 차단 해제하였습니다.")
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure

                    if (currentUserList.isEmpty()) {
                        _uiState.value = ManageMyUiState.Success(emptyList(), isLast = true)
                    } else {
                        _uiState.value = ManageMyUiState.Success(currentUserList.toList(), isLastPage)
                    }
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }
}