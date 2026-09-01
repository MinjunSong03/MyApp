package org.example.myapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.myapp.auth.model.PickedMedia
import org.example.myapp.auth.network.CreatePostRequest
import org.example.myapp.auth.network.MediaType
import org.example.myapp.auth.repository.MediaRepository
import org.example.myapp.auth.repository.PostRepository
import kotlin.coroutines.cancellation.CancellationException

class CreatePostViewModel(
    private val postRepository: PostRepository,
    private val mediaRepository: MediaRepository
): ViewModel() {
    private val _updateSuccessEvent = Channel<Unit>(Channel.BUFFERED)
    val updateSuccessEvent = _updateSuccessEvent.receiveAsFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun createPost(
        title: String,
        description: String,
        pickedMedia: PickedMedia
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uploadResult = mediaRepository.uploadMedia(pickedMedia).getOrThrow()
                val request = CreatePostRequest(
                    title = title,
                    description = description,
                    mediaType = uploadResult.mediaType,
                    thumbnailUrl = uploadResult.thumbnailUrl,
                    mediaUrl = uploadResult.mediaUrl
                )
                postRepository.createPost(request)
                    .onSuccess {
                        _toastEvent.send("게시물을 생성하였습니다.")
                        _updateSuccessEvent.send(Unit)
                    }
                    .onFailure { error ->
                        if (error is CancellationException) return@onFailure
                        _toastEvent.send(error.message ?: "게시물 생성에 실패했습니다.")
                    }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                _toastEvent.send(e.message ?: "미디어 업로드 처리에 실패했습니다.")
            } finally {
                _isLoading.value = false
            }
        }
    }
}