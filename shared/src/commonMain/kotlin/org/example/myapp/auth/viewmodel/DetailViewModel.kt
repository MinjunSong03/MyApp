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
import org.example.myapp.auth.repository.AuthRepository
import org.example.myapp.auth.repository.MediaRepository
import kotlin.coroutines.cancellation.CancellationException

class DetailViewModel(
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository
): ViewModel() {

    val authState = authRepository.authState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _updateSuccessEvent = Channel<Unit>(Channel.BUFFERED)
    val updateSuccessEvent = _updateSuccessEvent.receiveAsFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    fun updateProfile(
        nickname: String,
        selectedImage: PickedMedia?,
        deleteProfileImage: Boolean
    ) {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val uploadedImageUrl = if (selectedImage != null) {
                    val uploadResult = mediaRepository.uploadSingleImage(selectedImage)
                    uploadResult.getOrElse { error ->
                        val message = error.message ?: return@launch
                        _toastEvent.send(message)
                        return@launch
                    }
                } else null

                authRepository.updateProfile(
                    nickname = nickname,
                    profileImageUrl = uploadedImageUrl,
                    deleteProfileImage = deleteProfileImage
                ).onSuccess {
                    _toastEvent.send("프로필이 수정되었습니다.")
                    _updateSuccessEvent.send(Unit)
                }
                    .onFailure { error ->
                        val message = error.message ?: return@onFailure
                        _toastEvent.send(message)
                    }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val message = e.message ?: return@launch
                _toastEvent.send(message)
            } finally {
                _isLoading.value = false
            }
        }
    }
}