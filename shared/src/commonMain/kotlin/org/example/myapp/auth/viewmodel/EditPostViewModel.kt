package org.example.myapp.auth.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.myapp.auth.network.EditPostRequest
import org.example.myapp.auth.network.PostResponse
import org.example.myapp.auth.repository.PostRepository
import kotlin.coroutines.cancellation.CancellationException

data class EditPostUiState(
    val post: PostResponse? = null,
    val title: String = "",
    val description: String = "",
    val initialTitle: String = "",
    val initialDescription: String = "",
    val isInitialDataLoaded: Boolean = false,
    val isLoading: Boolean = false
) {
    val isContentChanged: Boolean
        get() = (title.trim() != initialTitle.trim()) || (description.trim() != initialDescription.trim())

    val isFormValid: Boolean
        get() = title.isNotBlank() && description.isNotBlank() && isContentChanged
}

class EditPostViewModel(
    savedStateHandle: SavedStateHandle,
    private val postRepository: PostRepository
): ViewModel() {
    val postId: Long = checkNotNull(savedStateHandle["postId"])
    private val _uiState = MutableStateFlow(EditPostUiState())
    val uiState: StateFlow<EditPostUiState> = _uiState.asStateFlow()

    private val _navigateBackEvent = Channel<Unit>(Channel.BUFFERED)
    val navigateBackEvent = _navigateBackEvent.receiveAsFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    init {
        loadPost()
    }

    private fun loadPost() {
        viewModelScope.launch {
            postRepository.getPostById(postId)
                .onSuccess { post ->
                    _uiState.update {
                        it.copy(
                            post = post,
                            title = post.title,
                            description = post.description,
                            initialTitle = post.title,
                            initialDescription = post.description,
                            isInitialDataLoaded = true
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _toastEvent.send(error.message ?: "게시물을 찾을 수 없습니다.")
                    _navigateBackEvent.send(Unit)
                }
        }
    }

    fun onTitleChange(newTitle: String) {
        if (newTitle.length <= 100) {
            _uiState.update { it.copy(title = newTitle) }
        }
    }

    fun onDescriptionChange(newDescription: String) {
        if (newDescription.length <= 3000) {
            _uiState.update { it.copy(description = newDescription) }
        }
    }

    fun editPost() {
        if (_uiState.value.isLoading) return
        val state = _uiState.value
        val post = state.post ?: return


        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val request = EditPostRequest(
                    title = state.title,
                    description = state.description,
                    videoUrl = post.videoUrl,
                    videoThumbnailUrl = post.videoThumbnailUrl,
                    imageUrls = post.imageUrls
                )
                postRepository.editPost(postId, request)
                    .onSuccess {
                        _toastEvent.send("게시물이 수정되었습니다.")
                        _navigateBackEvent.send(Unit)
                    }
                    .onFailure { error ->
                        if (error is CancellationException) return@onFailure
                        _toastEvent.send(error.message ?: "게시물 수정에 실패했습니다.")
                    }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}