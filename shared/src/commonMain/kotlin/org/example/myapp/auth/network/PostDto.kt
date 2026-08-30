package org.example.myapp.auth.network

import kotlinx.serialization.Serializable

enum class MediaType { IMAGE, VIDEO }
enum class ReportReason { SPAM, INAPPROPRIATE, VIOLENCE, COPYRIGHT, OTHER }

enum class UserStatus { ACTIVE, BANNED }

@Serializable
data class CreatePostRequest(
    val title: String,
    val description: String,
    val mediaType: MediaType,
    val thumbnailUrl: String,
    val mediaUrl: String
)

@Serializable
data class EditPostRequest(
    val title: String,
    val description: String,
    val mediaType: MediaType,
    val thumbnailUrl: String,
    val mediaUrl: String
)

@Serializable
data class PostResponse(
    val id: Long,
    val userId: Long,
    val authorNickname: String,
    val authorProfileImageUrl: String?,
    val title: String,
    val description: String,
    val mediaType: MediaType,
    val thumbnailUrl: String,
    val mediaUrl: String,
    val viewCount: Long,
    val createdAt: String,
    val isMine: Boolean
)

@Serializable
data class SliceResponse<T>(
    val content: List<T>,
    val last: Boolean
)

@Serializable
data class CreateReportRequest(
    val reason: ReportReason,
    val detail: String
)

@Serializable
data class BlockedUserResponse(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val status: UserStatus
)