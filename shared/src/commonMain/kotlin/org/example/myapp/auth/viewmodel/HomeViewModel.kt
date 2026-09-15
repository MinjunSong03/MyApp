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
            postRepository.homePosts.collect { posts ->
                if (_uiState.value !is HomeUiState.Loading || posts.isNotEmpty()) {
                    _uiState.value = HomeUiState.Success(posts, isLastPage)
                }
            }
        }
        loadHomeFeed(isRefresh = false)
    }

    fun loadHomeFeed(isRefresh: Boolean = false) {

        if (isRefresh) {
            feedJob?.cancel()
            _isRefreshing.value = true
            currentPage = 0
            isLastPage = false
        } else {
            if (isLastPage || feedJob?.isActive == true) return
            if (currentPostList.isEmpty()) {
                _uiState.value = HomeUiState.Loading
            }
        }

        val targetPage = if (isRefresh) 0 else currentPage

        feedJob = viewModelScope.launch {
            try {
                postRepository.getHomeFeed(targetPage, isRefresh)
                    .onSuccess { slice ->
                        isLastPage = slice.last
                        currentPage++
                        _uiState.value = HomeUiState.Success(postRepository.homePosts.value, isLastPage)
                    }
                    .onFailure { error ->
                        if (error is CancellationException) return@onFailure
                        if (_uiState.value !is HomeUiState.Success) {
                            _uiState.value = HomeUiState.Success(emptyList(), isLast = true)
                        }
                        val message = error.message ?: return@onFailure
                        _toastEvent.send(message)
                    }
            } catch (e : Exception) {
                if (e is CancellationException) throw e
                _uiState.value = HomeUiState.Success(postRepository.homePosts.value, isLastPage)
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
                    postRepository.removePostsByUserId(targetUserId)
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