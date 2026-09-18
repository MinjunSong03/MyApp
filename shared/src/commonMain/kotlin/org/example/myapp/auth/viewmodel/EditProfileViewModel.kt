package org.example.myapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.myapp.auth.model.AuthState
import org.example.myapp.auth.model.PickedMedia
import org.example.myapp.auth.repository.AuthRepository
import org.example.myapp.auth.repository.MediaRepository
import kotlin.coroutines.cancellation.CancellationException

data class EditProfileUiState(
    val selectedNickname: String = "",
    val selectedImage: PickedMedia? = null,
    val initialProfileImageUrl: String? = null,
    val initialNickname: String = "",
    val isImageDeleted: Boolean = false,
    val isLoading: Boolean = false
) {
    val isNicknameChanged: Boolean
        get() = selectedNickname.trim().isNotBlank() && selectedNickname.trim() != initialNickname

    val isImageChanged: Boolean
        get() = selectedImage != null || (isImageDeleted && initialProfileImageUrl != null)

    val isFormChanged: Boolean
        get() = isNicknameChanged || isImageChanged

    val isFormValid: Boolean
        get() = selectedNickname.trim().length in 2..10 && isFormChanged
}

class EditProfileViewModel(
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _updateSuccessEvent = Channel<Unit>(Channel.BUFFERED)
    val updateSuccessEvent = _updateSuccessEvent.receiveAsFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    fun onNicknameChange(newNickname: String) {
        if (newNickname.length <= 10) {
            _uiState.update { it.copy(selectedNickname = newNickname) }
        }
    }

    fun onImageChange(newImage: PickedMedia?) {
        _uiState.update { it.copy(selectedImage = newImage, isImageDeleted = false) }
    }

    fun onDeleteImageClick() {
        _uiState.update { it.copy(selectedImage = null, isImageDeleted = true) }
    }

    fun onRestoreImageClick() {
        _uiState.update { it.copy(isImageDeleted = false) }
    }

    init {
        viewModelScope.launch {
            val auth = authRepository.authState
                .filterIsInstance<AuthState.Authenticated>()
                .first()

            _uiState.update {
                it.copy(
                    selectedNickname = auth.session.nickname ?: "",
                    initialNickname = auth.session.nickname ?: "",
                    initialProfileImageUrl = auth.session.profileImageUrl
                )
            }
        }
    }


    fun updateProfile() {
        if (_uiState.value.isLoading) return
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val uploadedImageUrl = if (state.selectedImage != null) {
                    val uploadResult = mediaRepository.uploadSingleImage(state.selectedImage)
                    uploadResult.getOrElse { error ->
                        val message = error.message ?: return@launch
                        _toastEvent.send(message)
                        return@launch
                    }
                } else null

                authRepository.updateProfile(
                    nickname = state.selectedNickname,
                    profileImageUrl = uploadedImageUrl,
                    deleteProfileImage = state.isImageDeleted
                ).onSuccess {
                    _toastEvent.send("프로필이 수정되었습니다.")
                    _updateSuccessEvent.send(Unit)
                }.onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    val message = error.message ?: return@onFailure
                    _toastEvent.send(message)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val message = e.message ?: return@launch
                _toastEvent.send(message)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}