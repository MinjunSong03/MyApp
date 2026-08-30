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
import org.example.myapp.auth.network.EditPostRequest
import org.example.myapp.auth.network.MediaType
import org.example.myapp.auth.network.PostResponse
import org.example.myapp.auth.network.ReportReason
import org.example.myapp.auth.repository.PostRepository
import org.example.myapp.auth.repository.ReportRepository
import org.example.myapp.auth.repository.UserBlockRepository
import kotlin.coroutines.cancellation.CancellationException

sealed class MyPostUiState {
    object Loading: MyPostUiState()
    data class Success(val posts: List<PostResponse>, val isLast: Boolean): MyPostUiState()
}

sealed interface PostTab {
    object Act : PostTab
    object Hidden : PostTab
}

class MyPostViewModel(
    private val postRepository: PostRepository,
    private val userBlockRepository: UserBlockRepository,
    private val reportRepository: ReportRepository
): ViewModel() {
    private val _uiState = MutableStateFlow<MyPostUiState>(MyPostUiState.Loading)
    val uiState: StateFlow<MyPostUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private val _currentTab = MutableStateFlow<PostTab>(PostTab.Act)
    val currentTab: StateFlow<PostTab> = _currentTab.asStateFlow()

    private var currentPage = 0
    private var isLastPage = false
    private val currentPostList = mutableListOf<PostResponse>()

    private var feedJob: Job? = null

    fun switchTab(tab: PostTab) {
        if (_currentTab.value == tab) return
        _currentTab.value = tab
    }

    fun loadMyPost(isRefresh: Boolean = false) {

        if (isRefresh) {
            feedJob?.cancel()
            _isRefreshing.value = true
            currentPage = 0
            isLastPage = false
            currentPostList.clear()
            _uiState.value = MyPostUiState.Loading
        } else {
            if (isLastPage || feedJob?.isActive == true) return
        }

        feedJob = viewModelScope.launch {
            try {
                val result = when (_currentTab.value) {
                    PostTab.Act -> postRepository.getMyActPost(currentPage)
                    PostTab.Hidden -> postRepository.getMyHiddenPost(currentPage)
                }
                result.onSuccess { slice ->
                    if (isRefresh) currentPostList.clear()
                    currentPostList.addAll(slice.content)
                    isLastPage = slice.last
                    currentPage++
                    _uiState.value = MyPostUiState.Success(currentPostList.toList(), isLastPage)
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure

                    if (currentPostList.isEmpty()) {
                        _uiState.value = MyPostUiState.Success(emptyList(), isLast = true)
                    } else {
                        _uiState.value = MyPostUiState.Success(currentPostList.toList(), isLastPage)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                _toastEvent.send(e.message ?: "알 수 없는 오류가 발생했습니다.")
                _uiState.value = MyPostUiState.Success(currentPostList.toList(), isLastPage)
            } finally {
                if (isRefresh) {
                    _isRefreshing.value = false
                }
            }
        }
    }

    suspend fun getPostById(postId: Long): PostResponse? {
        currentPostList.firstOrNull { it.id == postId }?.let { return it }

        return postRepository.getPostById(postId)
            .onFailure { error ->
                if (error is CancellationException) return@onFailure
                _toastEvent.send(error.message ?: "게시물 가져오기에 실패했습니다.")
            }
            .getOrNull()
    }

    fun hidePost(postId: Long) {
        viewModelScope.launch {
            postRepository.hidePost(postId)
                .onSuccess {
                    currentPostList.removeAll { it.id == postId }
                    _uiState.value = MyPostUiState.Success(currentPostList.toList(), isLastPage)
                    _toastEvent.send("게시물을 숨김 처리하였습니다.")
                }
                .onFailure { error ->
                    _toastEvent.send(error.message ?: "게시물 숨김 처리에 실패했습니다.")
                }
        }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            postRepository.deletePost(postId)
                .onSuccess {
                    currentPostList.removeAll { it.id == postId }
                    _uiState.value = MyPostUiState.Success(currentPostList.toList(), isLastPage)
                    _toastEvent.send("게시물을 삭제하였습니다.")
                }
                .onFailure { error ->
                    _toastEvent.send(error.message ?: "게시물 삭제에 실패했습니다.")
                }
        }
    }

    fun blockUser(targetUserId: Long) {
        viewModelScope.launch {
            userBlockRepository.blockUser(targetUserId)
                .onSuccess {
                    currentPostList.removeAll { it.userId == targetUserId }
                    _uiState.value = MyPostUiState.Success(currentPostList.toList(), isLastPage)
                    _toastEvent.send("사용자를 차단하였습니다.")
                }
                .onFailure { error ->
                    _toastEvent.send(error.message ?: "사용자 차단에 실패했습니다.")
                }
        }
    }

    //fun unblockUser() {}

    fun reportPost(postId: Long, reason: ReportReason, detail: String) {
        viewModelScope.launch {
            reportRepository.reportPost(postId, reason, detail)
                .onSuccess { _toastEvent.send("신고가 접수되었습니다.") }
                .onFailure { error ->
                    _toastEvent.send(error.message ?: "신고 처리에 실패했습니다.")
                }
        }
    }
}