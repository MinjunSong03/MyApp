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
import org.example.myapp.auth.network.SliceResponse
import org.example.myapp.auth.repository.FeedType
import org.example.myapp.auth.repository.PostRepository
import kotlin.coroutines.cancellation.CancellationException

data class FeedTabState(
    val posts: List<PostResponse> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLast: Boolean = false,
    val page: Int = 0,
    val isLoaded: Boolean = false
)

data class MyPostUiState(
    val actFeed: FeedTabState = FeedTabState(),
    val hiddenFeed: FeedTabState = FeedTabState()
)

class MyPostViewModel(
    private val postRepository: PostRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(MyPostUiState())
    val uiState: StateFlow<MyPostUiState> = _uiState.asStateFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private var actJob: Job? = null
    private var hiddenJob: Job? = null

    init {
        viewModelScope.launch {
            postRepository.getFeedStream(FeedType.MY_ACT).collect { posts ->
                updateTabState(isAct = true) { it.copy(posts = posts) }
            }
        }

        viewModelScope.launch {
            postRepository.getFeedStream(FeedType.MY_HIDDEN).collect { posts ->
                updateTabState(isAct = false) { it.copy(posts = posts) }
            }
        }
        loadActFeed(isRefresh = false)
        loadHiddenFeed(isRefresh = false)
    }

    fun loadActFeed(isRefresh: Boolean) = loadFeed(
        isAct = true,
        feedType = FeedType.MY_ACT,
        isRefresh = isRefresh
    )

    fun loadHiddenFeed(isRefresh: Boolean) = loadFeed(
        isAct = false,
        feedType = FeedType.MY_HIDDEN,
        isRefresh = isRefresh
    )

    private fun loadFeed(
        isAct: Boolean,
        feedType: String,
        isRefresh: Boolean
    ) {
        val currentTabState = if (isAct) _uiState.value.actFeed else _uiState.value.hiddenFeed
        val activeJob = if (isAct) actJob else hiddenJob

        if (isRefresh) {
            activeJob?.cancel()
        } else {
            if (currentTabState.isLast || activeJob?.isActive == true) return
        }

        val targetPage = if (isRefresh) 0 else currentTabState.page
        val isFirstFetch = currentTabState.posts.isEmpty()

        updateTabState(isAct) {
            it.copy(
                isRefreshing = isRefresh,
                isInitialLoading = !isRefresh && isFirstFetch
            )
        }

        val job = viewModelScope.launch {
            try {
                postRepository.fetchFeed(feedType, targetPage, isRefresh)
                    .onSuccess { isLast ->
                        updateTabState(isAct) { prev ->
                            prev.copy(
                                page = targetPage + 1,
                                isLast = isLast,
                                isLoaded = true,
                                isInitialLoading = false,
                                isRefreshing = false
                            )
                        }
                    }
                    .onFailure { error ->
                        if (error is CancellationException) return@onFailure
                        updateTabState(isAct) { it.copy(isInitialLoading = false, isRefreshing = false) }
                        error.message?.let { _toastEvent.send(it) }
                    }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                updateTabState(isAct) { it.copy(isInitialLoading = false, isRefreshing = false) }
                e.message?.let { _toastEvent.send(it) }
            }
        }

        if (isAct) actJob = job else hiddenJob = job
    }

    private fun updateTabState(isAct: Boolean, transform: (FeedTabState) -> FeedTabState) {
        _uiState.update { current ->
            if (isAct) current.copy(actFeed = transform(current.actFeed))
            else current.copy(hiddenFeed = transform(current.hiddenFeed))
        }
    }

    fun hidePost(postId: Long) {
        viewModelScope.launch {
            postRepository.hidePost(postId)
                .onSuccess {
                    _toastEvent.send("게시물을 숨김 처리하였습니다.")
                    loadHiddenFeed(isRefresh = true)
                }
                .onFailure { error ->
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }

    fun unhidePost(postId: Long) {
        viewModelScope.launch {
            postRepository.unhidePost(postId)
                .onSuccess {
                    _toastEvent.send("게시물 숨김을 해제하였습니다.")
                    loadActFeed(isRefresh = true)
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


}