package org.example.myapp.auth.network

import kotlinx.serialization.Serializable

@Serializable
data class OAuthLoginRequest(
    val accessToken: String
)

@Serializable
data class UpdateProfileRequest(
    val nickname: String,
    val profileImageUrl: String?,
    val deleteProfileImage: Boolean
)

@Serializable
data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val isNewUser: Boolean
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String
)

@Serializable
data class ErrorResponse(
    val message: String,
    val code: String
)

@Serializable
data class UserProfileResponse(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String? = null,
    val isDeleted: Boolean = false,
    val isMine: Boolean = false
)