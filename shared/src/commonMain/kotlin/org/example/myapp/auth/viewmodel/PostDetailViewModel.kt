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
import org.example.myapp.auth.network.CommentResponse
import org.example.myapp.auth.network.PostResponse
import org.example.myapp.auth.network.ReportReason
import org.example.myapp.auth.repository.AuthRepository
import org.example.myapp.auth.repository.CommentRepository
import org.example.myapp.auth.repository.PostRepository
import org.example.myapp.auth.repository.ReportRepository
import org.example.myapp.auth.repository.UserBlockRepository
import kotlin.coroutines.cancellation.CancellationException

sealed class CommentUiState {
    object Loading : CommentUiState()
    data class Success(val comments: List<CommentResponse>, val isLast: Boolean) : CommentUiState()
}

class PostDetailViewModel(
    private val authRepository: AuthRepository,
    private val postRepository: PostRepository,
    private val userBlockRepository: UserBlockRepository,
    private val reportRepository: ReportRepository,
    private val commentRepository: CommentRepository

): ViewModel() {
    val authState = authRepository.authState

    private val _commentUiState = MutableStateFlow<CommentUiState>(CommentUiState.Loading)
    val commentUiState: StateFlow<CommentUiState> = _commentUiState.asStateFlow()

    private val _isCommentLoadingMore = MutableStateFlow(false)
    val isCommentLoadingMore: StateFlow<Boolean> = _isCommentLoadingMore.asStateFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private var commentPage = 0
    private var isCommentLastPage = false
    private val currentCommentList = mutableListOf<CommentResponse>()
    private var commentJob: Job? = null



    suspend fun getPostDetail(postId: Long): PostResponse? {
        return postRepository.getPostDetail(postId)
            .onFailure { error ->
                if (error is CancellationException) return@onFailure
                val message = error.message ?: return@onFailure
                _toastEvent.send(message)
            }
            .getOrNull()
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

    fun loadComments(postId: Long, isRefresh: Boolean = false) {

        if (isRefresh) {
            commentJob?.cancel()
            commentPage = 0
            isCommentLastPage = false
            currentCommentList.clear()
            _commentUiState.value = CommentUiState.Loading
        } else {
            if (isCommentLastPage || commentJob?.isActive == true) return
            _isCommentLoadingMore.value = true
        }

        commentJob = viewModelScope.launch {
            commentRepository.getComments(postId, commentPage)
                .onSuccess { slice ->
                    if (isRefresh) currentCommentList.clear()
                    val newItems = slice.content.filter { newItem ->
                        currentCommentList.none { it.id == newItem.id }
                    }
                    currentCommentList.addAll(newItems)
                    isCommentLastPage = slice.last
                    commentPage++
                    _commentUiState.value = CommentUiState.Success(currentCommentList.toList(), isCommentLastPage)
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    if (currentCommentList.isEmpty()) {
                        _commentUiState.value = CommentUiState.Success(emptyList(), isLast = true)
                    } else {
                        _commentUiState.value = CommentUiState.Success(currentCommentList.toList(), isCommentLastPage)
                    }
                }
            _isCommentLoadingMore.value = false
        }
    }

    fun createComment(postId: Long, content: String, onCreated: () -> Unit = {}) {
        viewModelScope.launch {
            commentRepository.createComment(postId, content)
                .onSuccess { newComment ->
                    if (currentCommentList.none { it.id == newComment.id }) {
                        currentCommentList.add(newComment)
                    }
                    _commentUiState.value = CommentUiState.Success(currentCommentList.toList(), isCommentLastPage)
                    onCreated()
                }
                .onFailure { error ->
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }

    fun editComment(commentId: Long, content: String) {
        viewModelScope.launch {
            commentRepository.editComment(commentId, content)
                .onSuccess { updatedComment ->
                    val index = currentCommentList.indexOfFirst { it.id == commentId }
                    if (index != -1) {
                        currentCommentList[index] = updatedComment
                        _commentUiState.value = CommentUiState.Success(currentCommentList.toList(), isCommentLastPage)
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch {
            commentRepository.deleteComment(commentId)
                .onSuccess {
                    currentCommentList.removeAll { it.id == commentId }
                    _commentUiState.value = CommentUiState.Success(currentCommentList.toList(), isCommentLastPage)
                    _toastEvent.send("댓글이 삭제되었습니다.")
                }
                .onFailure { error ->
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }

    fun reportComment(commentId: Long, reason: ReportReason, detail: String) {
        viewModelScope.launch {
            commentRepository.reportComment(commentId, reason, detail)
                .onSuccess { _toastEvent.send("댓글 신고가 접수되었습니다.") }
                .onFailure { error ->
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
        }
    }
}