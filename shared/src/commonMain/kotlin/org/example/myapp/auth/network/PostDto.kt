package org.example.myapp.auth.network

import kotlinx.serialization.Serializable

enum class MediaType { IMAGE, VIDEO }
enum class ReportReason { SPAM, INAPPROPRIATE, VIOLENCE, COPYRIGHT, OTHER }
enum class UserStatus { ACTIVE, BANNED, DELETED }

data class PostMediaItem(
    val mediaType: MediaType,
    val mediaUrl: String,
    val thumbnailUrl: String
)

@Serializable
data class CreatePostRequest(
    val title: String,
    val description: String,
    val videoUrl: String? = null,
    val videoThumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList()
)

@Serializable
data class EditPostRequest(
    val title: String,
    val description: String,
    val videoUrl: String? = null,
    val videoThumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList()
)

@Serializable
data class PostResponse(
    val id: Long,
    val userId: Long,
    val userNickname: String,
    val userProfileImageUrl: String?,
    val title: String,
    val description: String,
    val videoUrl: String? = null,
    val videoThumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val viewCount: Long,
    val createdAt: String,
    val isMine: Boolean,
    val isHidden: Boolean,
    val isUserDeleted: Boolean
) {
    val mediaItems: List<PostMediaItem>
        get() {
            val list = mutableListOf<PostMediaItem>()
            if (!videoUrl.isNullOrBlank()) {
                list.add(
                    PostMediaItem(
                        mediaType = MediaType.VIDEO,
                        mediaUrl = videoUrl,
                        thumbnailUrl = videoThumbnailUrl.orEmpty()
                    )
                )
            }
            imageUrls.forEach { url ->
                list.add(
                    PostMediaItem(
                        mediaType = MediaType.IMAGE,
                        mediaUrl = url,
                        thumbnailUrl = url
                    )
                )
            }
            return list
        }
}

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