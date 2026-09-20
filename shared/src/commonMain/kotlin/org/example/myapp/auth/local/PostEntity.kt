package org.example.myapp.auth.local

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val userNickname: String,
    val userProfileImageUrl: String?,
    val title: String,
    val description: String,
    val videoUrl: String?,
    val videoThumbnailUrl: String?,
    val imageUrls: String,
    val viewCount: Long,
    val createdAt: String,
    val editedAt: String?,
    val isMine: Boolean,
    val isHidden: Boolean,
    val isUserDeleted: Boolean
)

@Entity(
    tableName = "feed_items",
    primaryKeys = ["feedType", "postId"],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("postId")]
)
data class FeedItemEntity(
    val feedType: String,
    val postId: Long,
    val position: Int
)