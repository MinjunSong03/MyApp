package org.example.myapp.auth.repository

import kotlinx.coroutines.flow.StateFlow
import org.example.myapp.auth.model.AuthState
import org.example.myapp.auth.model.OAuthProvider

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun checkAutoLogin(): Result<Unit>
    suspend fun login(provider: OAuthProvider): Result<Unit>
    suspend fun logout(provider: OAuthProvider?): Result<Unit>
    suspend fun unlink(provider: OAuthProvider): Result<Unit>
    suspend fun updateProfile(
        nickname: String,
        profileImageUrl: String?,
        deleteProfileImage: Boolean
    ): Result<Unit>

}