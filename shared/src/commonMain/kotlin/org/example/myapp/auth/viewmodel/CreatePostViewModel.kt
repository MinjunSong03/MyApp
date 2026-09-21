package org.example.myapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.myapp.auth.model.PickedMedia
import org.example.myapp.auth.network.CreatePostRequest
import org.example.myapp.auth.repository.MediaRepository
import org.example.myapp.auth.repository.PostRepository
import kotlin.collections.emptyList
import kotlin.coroutines.cancellation.CancellationException

data class CreatePostUiState(
    val title: String = "",
    val description: String = "",
    val selectedVideo: PickedMedia? = null,
    val selectedImages: List<PickedMedia> = emptyList(),
    val isLoading: Boolean = false
) {
    val isFormValid: Boolean
        get() = title.isNotBlank() && description.isNotBlank()
}

class CreatePostViewModel(
    private val postRepository: PostRepository,
    private val mediaRepository: MediaRepository
): ViewModel() {
    private val _updateSuccessEvent = Channel<Unit>(Channel.BUFFERED)
    val updateSuccessEvent = _updateSuccessEvent.receiveAsFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState = _uiState.asStateFlow()

    fun clearForm() {
        _uiState.update { it.copy(
            title = "",
            description = "",
            selectedVideo = null,
            selectedImages = emptyList()
        ) }
    }

    fun onTitleChange(title: String) {
        if (title.length <= 100) {
            _uiState.update { it.copy(title = title) }
        }
    }

    fun onDescriptionChange(description: String) {
        if (description.length <= 3000) {
            _uiState.update { it.copy(description = description) }
        }
    }
    fun onVideoSelect(media: PickedMedia?) {
        _uiState.update { it.copy(selectedVideo = media) }
    }
    fun onImagesSelect(images: List<PickedMedia>) {
        _uiState.update { it.copy(selectedImages = images) }
    }

    fun createPost() {
        if (_uiState.value.isLoading) return
        val state = _uiState.value

        if (state.title.isBlank()) {
            viewModelScope.launch { _toastEvent.send("제목을 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val uploadResult = mediaRepository.uploadPostMedia(state.selectedVideo, state.selectedImages).getOrThrow()

                val request = CreatePostRequest(
                    title = state.title.trim(),
                    description = state.description.trim(),
                    videoUrl = uploadResult.videoUrl,
                    videoThumbnailUrl = uploadResult.videoThumbnailUrl,
                    imageUrls = uploadResult.imageUrls
                )

                postRepository.createPost(request)
                    .onSuccess {
                        clearForm()
                        _toastEvent.send("게시물을 생성하였습니다.")
                        _updateSuccessEvent.send(Unit)
                    }
                    .onFailure { error ->
                        if (error is CancellationException) return@onFailure
                        val message = error.message ?: return@onFailure
                        _toastEvent.send(message)
                    }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                val message = e.message ?: return@launch
                _toastEvent.send(message)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}