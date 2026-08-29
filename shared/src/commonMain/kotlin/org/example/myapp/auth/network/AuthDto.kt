package org.example.myapp.auth.network

import kotlinx.serialization.Serializable

@Serializable
data class OAuthLoginRequest(
    val accessToken: String
)

@Serializable
data class UpdateNicknameRequest(
    val nickname: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val isNewUser: Boolean
)

@Serializable
data class ErrorResponse(
    val message: String,
    val code: String
)