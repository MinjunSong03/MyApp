package org.example.myapp.auth.network

import kotlinx.serialization.Serializable

@Serializable
data class CreateCommentRequest(
    val postId: Long,
    val content: String
)

@Serializable
data class EditCommentRequest(
    val content: String
)

@Serializable
data class CommentResponse(
    val id: Long,
    val postId: Long,
    val userId: Long,
    val userNickname: String,
    val userProfileImageUrl: String?,
    val content: String,
    val createdAt: String,
    val editedAt: String?,
    val isMine: Boolean,
    val isUserDeleted: Boolean
)