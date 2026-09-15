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

sealed class MyPostUiState {
    object Loading: MyPostUiState()
    data class Success(val posts: List<PostResponse>, val isLast: Boolean): MyPostUiState()
}

sealed interface PostTab {
    object Act : PostTab
    object Hidden : PostTab
}

class MyPostViewModel(
    private val postRepository: PostRepository
): ViewModel() {
    private val _uiState = MutableStateFlow<MyPostUiState>(MyPostUiState.Loading)
    val uiState: StateFlow<MyPostUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private val _currentTab = MutableStateFlow<PostTab>(PostTab.Act)
    val currentTab: StateFlow<PostTab> = _currentTab.asStateFlow()

    private var feedJob: Job? = null

    private var actPage = 0
    private var isActLast = false
    private var isActLoaded = false

    private var hiddenPage = 0
    private var isHiddenLast = false
    private var isHiddenLoaded = false

    init {
        viewModelScope.launch {
            postRepository.myActPosts.collect { posts ->
                if (_currentTab.value is PostTab.Act && isActLoaded) {
                    _uiState.value = MyPostUiState.Success(posts, isActLast)
                }
            }
        }

        viewModelScope.launch {
            postRepository.myHiddenPosts.collect { posts ->
                if (_currentTab.value is PostTab.Hidden && isHiddenLoaded) {
                    _uiState.value = MyPostUiState.Success(posts, isHiddenLast)
                }
            }
        }

        loadMyPost(tab = PostTab.Act, isRefresh = false)
    }

    fun switchTab(tab: PostTab) {
        if (_currentTab.value == tab) return
        _currentTab.value = tab

        val isTargetLoaded = if (tab is PostTab.Act) isActLoaded else isHiddenLoaded
        val targetPosts = if (tab is PostTab.Act) postRepository.myActPosts.value else postRepository.myHiddenPosts.value
        val targetIsLast = if (tab is PostTab.Act) isActLast else isHiddenLast

        if (isTargetLoaded) {
            _uiState.value = MyPostUiState.Success(targetPosts.toList(), targetIsLast)
        } else {
            _uiState.value = MyPostUiState.Loading
            loadMyPost(tab = tab, isRefresh = false)
        }
    }

    fun loadMyPost(tab: PostTab = _currentTab.value, isRefresh: Boolean = false) {
        val isTargetLast = if (tab is PostTab.Act) isActLast else isHiddenLast
        val targetPosts = if (tab is PostTab.Act) postRepository.myActPosts.value else postRepository.myHiddenPosts.value

        if (isRefresh) {
            feedJob?.cancel()
            _isRefreshing.value = true
        } else {
            if (isTargetLast || feedJob?.isActive == true) return
            if (targetPosts.isEmpty()) {
                _uiState.value = MyPostUiState.Loading
            }
        }

        val targetPage = if (isRefresh) 0 else {
            if (tab is PostTab.Act) actPage else hiddenPage
        }

        feedJob = viewModelScope.launch {
            try {
                val result = when (tab) {
                    PostTab.Act -> postRepository.getMyActPost(targetPage, isRefresh)
                    PostTab.Hidden -> postRepository.getMyHiddenPost(targetPage, isRefresh)
                }
                result.onSuccess { slice ->
                    if (tab is PostTab.Act) {
                        if (isRefresh) actPage = 0
                        isActLast = slice.last
                        actPage++
                        isActLoaded = true
                    } else {
                        if (isRefresh) hiddenPage = 0
                        isHiddenLast = slice.last
                        hiddenPage++
                        isHiddenLoaded = true
                    }
                    if (_currentTab.value == tab) {
                        val currentList = if (tab is PostTab.Act) postRepository.myActPosts.value else postRepository.myHiddenPosts.value
                        val currentLast = if (tab is PostTab.Act) isActLast else isHiddenLast
                        _uiState.value = MyPostUiState.Success(currentList, currentLast)
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    if (_uiState.value !is MyPostUiState.Success) {
                        _uiState.value = MyPostUiState.Success(emptyList(), isLast = true)
                    }
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (_uiState.value !is MyPostUiState.Success) {
                    _uiState.value = MyPostUiState.Success(emptyList(), isLast = true)
                }
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

    fun unhidePost(post: PostResponse) {
        viewModelScope.launch {
            postRepository.unhidePost(post)
                .onSuccess {
                    _toastEvent.send("게시물 숨김을 해제하였습니다.")
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