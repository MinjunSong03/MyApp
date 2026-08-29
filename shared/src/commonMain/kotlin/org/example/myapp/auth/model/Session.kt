package org.example.myapp.auth.model

data class Session(
    val accessToken: String,
    val refreshToken: String? = null,
    val userId: Long? = null,
    val nickname: String? = null,
    val profileImageUrl: String? = null
)
