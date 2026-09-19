package org.example.myapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.myapp.auth.network.PostResponse
import org.example.myapp.auth.network.ReportReason
import org.example.myapp.auth.repository.FeedType
import org.example.myapp.auth.repository.PostRepository
import org.example.myapp.auth.repository.ReportRepository
import org.example.myapp.auth.repository.UserBlockRepository
import kotlin.coroutines.cancellation.CancellationException

data class HomeUiState(
    val posts: List<PostResponse> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLast: Boolean = false
) {
    val isEmpty: Boolean
        get() = !isInitialLoading && posts.isEmpty()
}

class HomeViewModel(
    private val postRepository: PostRepository,
    private val userBlockRepository: UserBlockRepository,
    private val reportRepository: ReportRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private var currentPage = 0
    private var isLastPage = false
    private var feedJob: Job? = null

    init {
        viewModelScope.launch {
            postRepository.getFeedStream(FeedType.HOME).collect { posts ->
                _uiState.update { it.copy(posts = posts) }
            }
        }
        loadHomeFeed(isRefresh = false)
    }

    fun loadHomeFeed(isRefresh: Boolean) {
        if (isRefresh) {
            feedJob?.cancel()
            _uiState.update { it.copy(isRefreshing = true) }
            currentPage = 0
            isLastPage = false
        } else {
            if (isLastPage || feedJob?.isActive == true) return
            if (_uiState.value.posts.isEmpty()) {
                _uiState.update { it.copy(isInitialLoading = true) }
            }
        }

        val targetPage = if (isRefresh) 0 else currentPage

        feedJob = viewModelScope.launch {
            try {
                postRepository.fetchFeed(FeedType.HOME, targetPage, isRefresh)
                    .onSuccess { isLast ->
                        isLastPage = isLast
                        currentPage = targetPage + 1
                        _uiState.update { it.copy(isLast = isLastPage) }
                    }
                    .onFailure { error ->
                        if (error is CancellationException) return@onFailure
                        val message = error.message ?: return@onFailure
                        _toastEvent.send(message)
                    }
            } catch (e : Exception) {
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
                .onSuccess {
                    _toastEvent.send("신고가 접수되었습니다.")
                }
                .onFailure { error ->
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }

    fun reportUser(targetId: Long, reason: ReportReason, detail: String) {
        viewModelScope.launch {
            reportRepository.reportUser(targetId, reason, detail)
                .onSuccess {
                    _toastEvent.send("신고가 접수되었습니다.")
                }
                .onFailure { error ->
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }
}