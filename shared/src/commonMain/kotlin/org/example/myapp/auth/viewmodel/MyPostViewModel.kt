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
    val page: Int = 0
)


class MyPostViewModel(
    private val postRepository: PostRepository
): ViewModel() {
    private val _actFeed = MutableStateFlow(FeedTabState())
    val actFeed = _actFeed.asStateFlow()

    private val _hiddenFeed = MutableStateFlow(FeedTabState())
    val hiddenFeed = _hiddenFeed.asStateFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private var actJob: Job? = null
    private var hiddenJob: Job? = null

    init {
        viewModelScope.launch {
            postRepository.getFeedStream(FeedType.MyAct).collect { posts ->
                _actFeed.update { it.copy(posts = posts) }
            }
        }

        viewModelScope.launch {
            postRepository.getFeedStream(FeedType.MyHidden).collect { posts ->
                _hiddenFeed.update { it.copy(posts = posts) }
            }
        }

        viewModelScope.launch {
            postRepository.feedRefreshEvent.collect { feedType ->
                if (feedType is FeedType.MyAct) {
                    loadActFeed(isRefresh = true)
                }
            }
        }
        loadActFeed(isRefresh = false)
        loadHiddenFeed(isRefresh = false)
    }

    fun loadActFeed(isRefresh: Boolean) = loadFeed(
        isAct = true,
        feedType = FeedType.MyAct,
        isRefresh = isRefresh
    )

    fun loadHiddenFeed(isRefresh: Boolean) = loadFeed(
        isAct = false,
        feedType = FeedType.MyHidden,
        isRefresh = isRefresh
    )

    private fun loadFeed(
        isAct: Boolean,
        feedType: FeedType,
        isRefresh: Boolean
    ) {
        val currentTabState = if (isAct) _actFeed else _hiddenFeed
        val activeJob = if (isAct) actJob else hiddenJob

        if (isRefresh) {
            activeJob?.cancel()
            currentTabState.update {
                it.copy(
                    isRefreshing = true,
                    isLast = false
                )
            }
        } else {
            if (currentTabState.value.isLast || activeJob?.isActive == true) return
            if (currentTabState.value.posts.isEmpty()) {
                currentTabState.update { it.copy(isInitialLoading = true) }
            }
        }

        val targetPage = if (isRefresh) 0 else currentTabState.value.page

        val job = viewModelScope.launch {
            postRepository.fetchFeed(feedType, targetPage, isRefresh)
                .onSuccess { isLast ->
                    currentTabState.update {
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
                    currentTabState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
                    error.message?.let { _toastEvent.send(it) }
                }
        }
        if (isAct) actJob = job else hiddenJob = job
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