package org.example.myapp.auth.viewmodel

import androidx.lifecycle.SavedStateHandle
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
import org.example.myapp.auth.network.UserProfileResponse
import org.example.myapp.auth.repository.FeedType
import org.example.myapp.auth.repository.PostRepository
import org.example.myapp.auth.repository.ReportRepository
import org.example.myapp.auth.repository.UserBlockRepository
import kotlin.coroutines.cancellation.CancellationException

data class ProfileClickUiState(
    val posts: List<PostResponse> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLast: Boolean = false,
    val page: Int = 0,
    val userProfileResponse: UserProfileResponse? = null
)

class ProfileClickViewModel(
    savedStateHandle: SavedStateHandle,
    private val postRepository: PostRepository,
    private val userBlockRepository: UserBlockRepository,
    private val reportRepository: ReportRepository
): ViewModel() {
    val userId: Long = checkNotNull(savedStateHandle["userId"])
    private val _uiState = MutableStateFlow(ProfileClickUiState())
    val uiState: StateFlow<ProfileClickUiState> = _uiState.asStateFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()
    private var feedJob: Job? = null

    init {
        viewModelScope.launch {
            postRepository.getFeedStream(FeedType.UserPosts(userId)).collect { posts ->
                _uiState.update { it.copy(posts = posts) }
            }
        }
        loadProfile()
        loadPost(isRefresh = false)
    }

    fun loadProfile() {
        viewModelScope.launch {
            postRepository.getUserProfile(userId)
                .onSuccess { profile ->
                    _uiState.update { it.copy(userProfileResponse = profile) }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }

    fun loadPost(isRefresh: Boolean) {
        if (userId == 0L) return

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
            if (_uiState.value.posts.isEmpty()) {
                _uiState.update { it.copy(isInitialLoading = true) }
            }
        }

        val targetPage = if (isRefresh) 0 else _uiState.value.page

        feedJob = viewModelScope.launch {
            postRepository.fetchFeed(FeedType.UserPosts(userId), targetPage, isRefresh)
                .onSuccess { isLast ->
                    _uiState.update {
                        it.copy(
                            page = targetPage + 1,
                            isLast = isLast,
                            isInitialLoading = false,
                            isRefreshing = false
                        )
                    }
                }.onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }

    fun toggleLikePost(postId: Long) {
        val targetPost = _uiState.value.posts.find { it.id == postId } ?: return
        val isLiked = targetPost.isLiked

        viewModelScope.launch {
            val result = if (isLiked) {
                postRepository.unlikePost(postId)
            } else {
                postRepository.likePost(postId)
            }

            result.onFailure { error ->
                if (error is CancellationException) return@onFailure
                val message = error.message ?: return@onFailure
                _toastEvent.send(message)
            }
        }
    }

    fun toggleLikeUser() {
        val currentProfile = _uiState.value.userProfileResponse ?: return
        val willLike = !currentProfile.isLiked
        val prevCount = currentProfile.likeCount
        val optimisticCount = if (willLike) prevCount + 1 else (prevCount - 1).coerceAtLeast(0)

        _uiState.update {
            it.copy(
                userProfileResponse = currentProfile.copy(
                    isLiked = willLike,
                    likeCount = optimisticCount
                )
            )
        }

        viewModelScope.launch {
            val result = if (willLike) {
                postRepository.likeUser(userId)
            } else {
                postRepository.unlikeUser(userId)
            }

            result.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        userProfileResponse = _uiState.value.userProfileResponse?.copy(
                            isLiked = response.isLiked,
                            likeCount = response.likeCount
                        )
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        userProfileResponse = currentProfile.copy(
                            isLiked = currentProfile.isLiked,
                            likeCount = prevCount
                        )
                    )
                }
                val message = error.message ?: return@onFailure
                _toastEvent.send(message)
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