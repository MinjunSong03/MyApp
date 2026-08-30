package org.example.myapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.myapp.auth.network.CreatePostRequest
import org.example.myapp.auth.network.MediaType
import org.example.myapp.auth.repository.PostRepository
import kotlin.coroutines.cancellation.CancellationException

class CreatePostViewModel(
    private val postRepository: PostRepository
): ViewModel() {

    private val _updateSuccessEvent = Channel<Unit>(Channel.BUFFERED)
    val updateSuccessEvent = _updateSuccessEvent.receiveAsFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    fun createPost(
        title: String,
        description: String,
        mediaType: MediaType,
        thumbnailUrl: String,
        mediaUrl: String
    ) {
        viewModelScope.launch {
            val request = CreatePostRequest(
                title = title,
                description = description,
                mediaType = mediaType,
                thumbnailUrl = thumbnailUrl,
                mediaUrl = mediaUrl
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
        }
    }
}