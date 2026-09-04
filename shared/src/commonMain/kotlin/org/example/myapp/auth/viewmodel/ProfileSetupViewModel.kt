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

class ProfileSetupViewModel(
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository
): ViewModel() {
    val authState = authRepository.authState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    fun updateProfile(
        nickname: String,
        selectedImage: PickedMedia?
    ) {
        if (_isLoading.value) return

        viewModelScope.launch {
            try {
                val uploadedImageUrl = if (selectedImage != null) {
                    val uploadResult = mediaRepository.uploadSingleImage(selectedImage)
                    uploadResult.getOrElse { error ->
                        _toastEvent.send(error.message ?: "프로필 사진 업로드에 실패했습니다.")
                        return@launch
                    }
                } else null

                authRepository.updateProfile(
                    nickname = nickname.trim(),
                    profileImageUrl = uploadedImageUrl,
                    deleteProfileImage = false
                ).onSuccess {
                    _toastEvent.send("프로필이 설정되었습니다.")
                }.onFailure { error ->
                        _toastEvent.send(error.message ?: "프로필 설정에 실패했습니다.")
                    }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _toastEvent.send("프로필 설정 중 오류가 발생했습니다.")
            } finally {
                _isLoading.value = false
            }
        }
    }
}