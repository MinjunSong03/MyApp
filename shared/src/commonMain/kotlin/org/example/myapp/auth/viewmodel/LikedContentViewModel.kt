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
import org.example.myapp.auth.network.UserResponse
import org.example.myapp.auth.repository.FeedType
import org.example.myapp.auth.repository.PostRepository
import org.example.myapp.auth.repository.ReportRepository
import org.example.myapp.auth.repository.UserBlockRepository
import kotlin.coroutines.cancellation.CancellationException

data class LikedUsersTabState(
    val users: List<UserResponse> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLast: Boolean = false,
    val page: Int = 0
) {
    val isEmpty: Boolean get() = !isInitialLoading && users.isEmpty()
}

data class LikedPostsTabState(
    val posts: List<PostResponse> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLast: Boolean = false,
    val page: Int = 0
) {
    val isEmpty: Boolean get() = !isInitialLoading && posts.isEmpty()
}

class LikedContentViewModel(
    private val postRepository: PostRepository,
    private val userBlockRepository: UserBlockRepository,
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _usersState = MutableStateFlow(LikedUsersTabState())
    val usersState: StateFlow<LikedUsersTabState> = _usersState.asStateFlow()

    private val _postsState = MutableStateFlow(LikedPostsTabState())
    val postsState: StateFlow<LikedPostsTabState> = _postsState.asStateFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private var usersJob: Job? = null
    private var postsJob: Job? = null

    init {
        viewModelScope.launch {
            postRepository.getFeedStream(FeedType.Liked).collect { posts ->
                _postsState.update { it.copy(posts = posts) }
            }
        }

        viewModelScope.launch {
            postRepository.feedRefreshEvent.collect { feedType ->
                if (feedType is FeedType.Liked) {
                    loadLikedPosts(isRefresh = true)
                }
            }
        }

        loadLikedUsers(isRefresh = false)
        loadLikedPosts(isRefresh = false)
    }

    fun loadLikedUsers(isRefresh: Boolean) {
        if (isRefresh) {
            usersJob?.cancel()
            _usersState.update {
                it.copy(
                    isRefreshing = true,
                    isLast = false
                ) }
        } else {
            if (_usersState.value.isLast || usersJob?.isActive == true) return
            if (_usersState.value.users.isEmpty()) {
                _usersState.update { it.copy(isInitialLoading = true) }
            }
        }

        val targetPage = if (isRefresh) 0 else _usersState.value.page

        usersJob = viewModelScope.launch {
            postRepository.fetchLikedUsers(targetPage)
                .onSuccess { slice ->
                    _usersState.update { current ->
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
                    _usersState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
                    error.message?.let { _toastEvent.send(it) }
                }
        }
    }

    fun toggleLikeUser(userId: Long) {
        viewModelScope.launch {
            val originalUsers = _usersState.value.users
            _usersState.update { current ->
                current.copy(users = current.users.filterNot { it.id == userId })
            }

            postRepository.unlikeUser(userId)
                .onSuccess {
                    _toastEvent.send("사용자 좋아요를 취소하였습니다.")
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _usersState.update { it.copy(users = originalUsers) }
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }

    fun loadLikedPosts(isRefresh: Boolean) {
        if (isRefresh) {
            postsJob?.cancel()
            _postsState.update {
                it.copy(
                    isRefreshing = true,
                    isLast = false
                )
            }
        } else {
            if (_postsState.value.isLast || postsJob?.isActive == true) return
            if (_postsState.value.posts.isEmpty()) {
                _postsState.update { it.copy(isInitialLoading = true) }
            }
        }

        val targetPage = if (isRefresh) 0 else _postsState.value.page

        postsJob = viewModelScope.launch {
            postRepository.fetchFeed(FeedType.Liked, targetPage, isRefresh)
                .onSuccess { isLast ->
                    _postsState.update {
                        it.copy(
                            page = targetPage + 1,
                            isLast = isLast,
                            isInitialLoading = false,
                            isRefreshing = false
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _postsState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
                    error.message?.let { _toastEvent.send(it) }
                }
        }
    }

    fun toggleLikePost(postId: Long) {
        viewModelScope.launch {
            postRepository.unlikePost(postId)
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }

    fun hidePost(postId: Long) {
        viewModelScope.launch {
            postRepository.hidePost(postId)
                .onSuccess {
                    _postsState.update { current ->
                        current.copy(posts = current.posts.filterNot { it.id == postId })
                    }
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
                    _postsState.update { current ->
                        current.copy(posts = current.posts.filterNot { it.id == postId })
                    }
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
                    _postsState.update { current ->
                        current.copy(posts = current.posts.filterNot { it.userId == targetUserId })
                    }
                    _usersState.update { current ->
                        current.copy(users = current.users.filterNot { it.id == targetUserId })
                    }
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