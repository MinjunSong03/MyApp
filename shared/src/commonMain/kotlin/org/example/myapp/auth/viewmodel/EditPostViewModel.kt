package org.example.myapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.myapp.auth.network.EditPostRequest
import org.example.myapp.auth.network.MediaType
import org.example.myapp.auth.network.PostResponse
import org.example.myapp.auth.repository.PostRepository
import kotlin.coroutines.cancellation.CancellationException

class EditPostViewModel(
    private val postRepository: PostRepository
): ViewModel() {
    private val _updateSuccessEvent = Channel<Unit>(Channel.BUFFERED)
    val updateSuccessEvent = _updateSuccessEvent.receiveAsFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    suspend fun getPostById(postId: Long): PostResponse? {
        return postRepository.getPostById(postId)
            .onFailure { error ->
                if (error is CancellationException) return@onFailure
                _toastEvent.send(error.message ?: "게시물 가져오기에 실패했습니다.")
            }
            .getOrNull()
    }

    fun editPost(
        postId: Long,
        title: String,
        description: String,
        mediaType: MediaType,
        thumbnailUrl: String,
        mediaUrl: String
    ) {
        viewModelScope.launch {

            val request = EditPostRequest(
                title = title,
                description = description,
                mediaType = mediaType,
                thumbnailUrl = thumbnailUrl,
                mediaUrl = mediaUrl
            )
            postRepository.editPost(postId, request)
                .onSuccess {
                    _toastEvent.send("게시물이 수정되었습니다.")
                    _updateSuccessEvent.send(Unit)
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _toastEvent.send(error.message ?: "게시물 수정에 실패했습니다.")
                }
        }
    }
}