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
import org.example.myapp.auth.network.CommentResponse
import org.example.myapp.auth.network.PostResponse
import org.example.myapp.auth.network.ReportReason
import org.example.myapp.auth.repository.AuthRepository
import org.example.myapp.auth.repository.CommentRepository
import org.example.myapp.auth.repository.PostRepository
import org.example.myapp.auth.repository.ReportRepository
import org.example.myapp.auth.repository.UserBlockRepository
import kotlin.coroutines.cancellation.CancellationException

data class PostDetailUiState(
    val post: PostResponse? = null,
    val isLoading: Boolean = false,
)

data class CommentUiState(
    val comments: List<CommentResponse> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLast: Boolean = false,
    val page: Int = 0,
    val commentText: String = "",
    val editingComment: CommentResponse? = null
) {
    val isEmpty: Boolean
        get() = !isInitialLoading && comments.isEmpty()
}

class PostDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val postRepository: PostRepository,
    private val userBlockRepository: UserBlockRepository,
    private val reportRepository: ReportRepository,
    private val commentRepository: CommentRepository

): ViewModel() {
    val postId: Long = checkNotNull(savedStateHandle["postId"])

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val _commentUiState = MutableStateFlow(CommentUiState())
    val commentUiState: StateFlow<CommentUiState> = _commentUiState.asStateFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private var commentJob: Job? = null

    init {
        viewModelScope.launch {
            postRepository.getPostStream(postId).collect { post ->
                _uiState.update { it.copy(post = post) }
            }
        }
        fetchPostDetail()
    }

    fun onCommentTextChanged(text: String) {
        if (text.length <= 500) {
            _commentUiState.update { it.copy(commentText = text) }
        }
    }

    fun startEditComment(comment: CommentResponse) {
        _commentUiState.update {
            it.copy(
                editingComment = comment,
                commentText = comment.content
            )
        }
    }

    fun cancelEditComment() {
        _commentUiState.update {
            it.copy(
                editingComment = null,
                commentText = ""
            )
        }
    }


    private fun fetchPostDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            postRepository.getPostDetail(postId)
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
            _uiState.update { it.copy(isLoading = false) }
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

    fun unhidePost(postId: Long) {
        viewModelScope.launch {
            postRepository.unhidePost(postId)
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

    // comment
    fun loadComments(isRefresh: Boolean) {
        if (isRefresh) {
            commentJob?.cancel()
            _commentUiState.update {
                it.copy(
                    isRefreshing = true,
                    isLast = false
                )
            }
        } else {
            if (_commentUiState.value.isLast || commentJob?.isActive == true) return
            if (_commentUiState.value.comments.isEmpty()) {
                _commentUiState.update { it.copy(isInitialLoading = true) }
            }
        }

        val targetPage = if (isRefresh) 0 else _commentUiState.value.page

        commentJob = viewModelScope.launch {
            commentRepository.getComments(postId, targetPage)
                .onSuccess { slice ->
                    if (isRefresh) _commentUiState.update { it.copy(comments = emptyList()) }
                    _commentUiState.update { current ->
                        val comments = if (isRefresh) {
                            slice.content
                        } else {
                            val existingIds = current.comments.map { it.id }.toSet()
                            current.comments + slice.content.filter { it.id !in existingIds }
                        }
                        current.copy(
                            comments = comments,
                            page = targetPage + 1,
                            isLast = slice.last,
                            isInitialLoading = false,
                            isRefreshing = false
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _commentUiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
                    error.message?.let { _toastEvent.send(it) }
                }
        }
    }

    fun createComment(postId: Long, content: String) {
        viewModelScope.launch {
            commentRepository.createComment(postId, content)
                .onSuccess { newComment ->
                    _commentUiState.update { current ->
                        current.copy(
                            comments = listOf(newComment) + current.comments,
                            commentText = ""
                        )
                    }
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
                    _commentUiState.update { current ->
                        current.copy(
                            comments = current.comments.map {
                                if (it.id == commentId) updatedComment else it
                            },
                            editingComment = null,
                            commentText = ""
                        )
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
                    _commentUiState.update { current ->
                        current.copy(comments = current.comments.filterNot { it.id == commentId })
                    }
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