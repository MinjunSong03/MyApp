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
import org.example.myapp.auth.network.CreatePostRequest
import org.example.myapp.auth.network.EditPostRequest
import org.example.myapp.auth.network.MediaType
import org.example.myapp.auth.network.PostResponse
import org.example.myapp.auth.network.ReportReason
import org.example.myapp.auth.repository.PostRepository
import org.example.myapp.auth.repository.ReportRepository
import org.example.myapp.auth.repository.UserBlockRepository
import kotlin.coroutines.cancellation.CancellationException

sealed class PostUiState {
    object Loading: PostUiState()
    data class Success(val posts: List<PostResponse>, val isLast: Boolean): PostUiState()
}

class PostViewModel(
    private val postRepository: PostRepository,
    private val userBlockRepository: UserBlockRepository,
    private val reportRepository: ReportRepository
): ViewModel() {
    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _updateSuccessEvent = Channel<Unit>(Channel.BUFFERED)
    val updateSuccessEvent = _updateSuccessEvent.receiveAsFlow()

    private val _toastEvent = Channel<String>()
    val toastEvent = _toastEvent.receiveAsFlow()

    private var currentPage = 0
    private var isLastPage = false
    private val currentPostList = mutableListOf<PostResponse>()

    private var feedJob: Job? = null
    private var createPostJob: Job? = null

    fun loadHomeFeed(isRefresh: Boolean = false) {

        if (isRefresh) {
            feedJob?.cancel()
            _isRefreshing.value = true
            currentPage = 0
            isLastPage = false
            currentPostList.clear()
            _uiState.value = PostUiState.Loading
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
                        _uiState.value = PostUiState.Success(currentPostList.toList(), isLastPage)
                    }
                    .onFailure { error ->
                        if (error is CancellationException) return@onFailure

                        if (currentPostList.isEmpty()) {
                            _uiState.value = PostUiState.Success(emptyList(), isLast = true)
                        } else {
                            _uiState.value = PostUiState.Success(currentPostList.toList(), isLastPage)
                        }
                    }
            } catch (e : Exception) {
                if (e is CancellationException) throw e

                _toastEvent.send(e.message ?: "알 수 없는 오류가 발생했습니다.")
                _uiState.value = PostUiState.Success(currentPostList.toList(), isLastPage)
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

    fun createPost(
        title: String,
        description: String,
        mediaType: MediaType,
        thumbnailUrl: String,
        mediaUrl: String
    ) {
        if (createPostJob?.isActive == true) return

        createPostJob = viewModelScope.launch {
            val request = CreatePostRequest(
                title = title,
                description = description,
                mediaType = mediaType,
                thumbnailUrl = thumbnailUrl,
                mediaUrl = mediaUrl
            )
            postRepository.createPost(request)
                .onSuccess { newPost ->
                    currentPostList.add(0, newPost)
                    _toastEvent.send("게시물을 생성하였습니다.")
                    _updateSuccessEvent.send(Unit)
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _toastEvent.send(error.message ?: "게시물 생성에 실패했습니다.")
                }
        }
    }

    fun editPost(
        postId: Long,
        title: String,
        description: String,
        mediaType: MediaType,
        thumbnailUrl: String,
        mediaUrl: String
    ) {
        viewModelScope.launch {

            val request = EditPostRequest(
                title = title,
                description = description,
                mediaType = mediaType,
                thumbnailUrl = thumbnailUrl,
                mediaUrl = mediaUrl
            )
            postRepository.editPost(postId, request)
                .onSuccess { editedPost ->
                    val index = currentPostList.indexOfFirst { it.id == postId }
                    if (index != -1) {
                        currentPostList[index] = editedPost
                        _uiState.value = PostUiState.Success(currentPostList.toList(), isLastPage)
                    }
                    _toastEvent.send("게시물이 수정되었습니다.")
                    _updateSuccessEvent.send(Unit)
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _toastEvent.send(error.message ?: "게시물 수정에 실패했습니다.")
                }
        }
    }

    fun hidePost(postId: Long) {
        viewModelScope.launch {
            postRepository.hidePost(postId)
                .onSuccess {
                    currentPostList.removeAll { it.id == postId }
                    _uiState.value = PostUiState.Success(currentPostList.toList(), isLastPage)
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
                    _uiState.value = PostUiState.Success(currentPostList.toList(), isLastPage)
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
                    _uiState.value = PostUiState.Success(currentPostList.toList(), isLastPage)
                    _toastEvent.send("사용자를 차단하였습니다.")
                }
                .onFailure { error ->
                    _toastEvent.send(error.message ?: "사용자 차단에 실패했습니다.")
                }
        }
    }

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