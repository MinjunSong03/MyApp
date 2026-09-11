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
import org.example.myapp.auth.network.PostResponse
import org.example.myapp.auth.network.ReportReason
import org.example.myapp.auth.repository.PostRepository
import org.example.myapp.auth.repository.ReportRepository
import org.example.myapp.auth.repository.UserBlockRepository
import kotlin.coroutines.cancellation.CancellationException

sealed class HomeUiState {
    object Loading: HomeUiState()
    data class Success(val posts: List<PostResponse>, val isLast: Boolean): HomeUiState()
}

class HomeViewModel(
    private val postRepository: PostRepository,
    private val userBlockRepository: UserBlockRepository,
    private val reportRepository: ReportRepository
): ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private var currentPage = 0
    private var isLastPage = false
    private val currentPostList = mutableListOf<PostResponse>()

    private var feedJob: Job? = null

    init {
        viewModelScope.launch {
            postRepository.postEditedEvent.collect { editedPost ->
                val index = currentPostList.indexOfFirst { it.id == editedPost.id }
                if (index != -1) {
                    currentPostList[index] = editedPost
                    _uiState.value = HomeUiState.Success(currentPostList.toList(), isLastPage)
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            postRepository.createPostEvent.collect { newPost ->
                currentPostList.add(0, newPost)
                _uiState.value = HomeUiState.Success(currentPostList.toList(), isLastPage)
            }
        }
    }

    init {
        viewModelScope.launch {
            postRepository.postHiddenEvent.collect { hiddenId ->
                currentPostList.removeAll { it.id == hiddenId }
                _uiState.value = HomeUiState.Success(currentPostList.toList(), isLastPage)
            }
        }
        viewModelScope.launch {
            postRepository.postDeletedEvent.collect { deletedId ->
                currentPostList.removeAll { it.id == deletedId }
                _uiState.value = HomeUiState.Success(currentPostList.toList(), isLastPage)
            }
        }

        viewModelScope.launch {
            postRepository.postUnhiddenEvent.collect { unhiddenPost ->
                if (currentPostList.any { it.id == unhiddenPost.id }) return@collect

                val targetIndex = currentPostList.indexOfFirst { it.createdAt < unhiddenPost.createdAt }
                if (targetIndex != -1) {
                    currentPostList.add(targetIndex, unhiddenPost)
                    _uiState.value = HomeUiState.Success(currentPostList.toList(), isLastPage)
                } else if (isLastPage) {
                    currentPostList.add(unhiddenPost)
                    _uiState.value = HomeUiState.Success(currentPostList.toList(), isLastPage)
                }
            }
        }
    }

    fun loadHomeFeed(isRefresh: Boolean = false) {

        if (isRefresh) {
            feedJob?.cancel()
            _isRefreshing.value = true
            currentPage = 0
            isLastPage = false
            currentPostList.clear()
            _uiState.value = HomeUiState.Loading
        } else {
            if (isLastPage || feedJob?.isActive == true) return
        }

        feedJob = viewModelScope.launch {
            try {
                postRepository.getHomeFeed(currentPage)
                    .onSuccess { slice ->
                        if (isRefresh) currentPostList.clear()
                        currentPostList.addAll(slice.content)
                        isLastPage = slice.last
                        currentPage++
                        _uiState.value = HomeUiState.Success(currentPostList.toList(), isLastPage)
                    }
                    .onFailure { error ->
                        if (error is CancellationException) return@onFailure
                        if (currentPostList.isEmpty()) {
                            _uiState.value = HomeUiState.Success(emptyList(), isLast = true)
                        } else {
                            _uiState.value = HomeUiState.Success(currentPostList.toList(), isLastPage)
                        }
                        val message = error.message ?: return@onFailure
                        _toastEvent.send(message)
                    }
            } catch (e : Exception) {
                if (e is CancellationException) throw e
                _uiState.value = HomeUiState.Success(currentPostList.toList(), isLastPage)
                val message = e.message ?: return@launch
                _toastEvent.send(message)
            } finally {
                if (isRefresh) {
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun hidePost(postId: Long) {
        viewModelScope.launch {
            postRepository.hidePost(postId)
                .onSuccess {
                    _toastEvent.send("게시물을 숨김 처리하였습니다.")
                }
                .onFailure { error ->
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            postRepository.deletePost(postId)
                .onSuccess {
                    _toastEvent.send("게시물을 삭제하였습니다.")
                }
                .onFailure { error ->
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }

    fun blockUser(targetUserId: Long) {
        viewModelScope.launch {
            userBlockRepository.blockUser(targetUserId)
                .onSuccess {
                    currentPostList.removeAll { it.userId == targetUserId }
                    _uiState.value = HomeUiState.Success(currentPostList.toList(), isLastPage)
                    _toastEvent.send("사용자를 차단하였습니다.")
                }
                .onFailure { error ->
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }

    fun reportPost(postId: Long, reason: ReportReason, detail: String) {
        viewModelScope.launch {
            reportRepository.reportPost(postId, reason, detail)
                .onSuccess { _toastEvent.send("신고가 접수되었습니다.") }
                .onFailure { error ->
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }

    fun reportUser(targetId: Long, reason: ReportReason, detail: String) {
        viewModelScope.launch {
            reportRepository.reportUser(targetId, reason, detail)
                .onSuccess { _toastEvent.send("신고가 접수되었습니다.") }
                .onFailure { error ->
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }
}