package org.example.myapp.auth.repository

import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.example.myapp.auth.local.PostEntity
import org.example.myapp.auth.local.PostDao
import org.example.myapp.auth.network.*

sealed interface FeedType {
    val storageKey: String

    data object Home : FeedType {
        override val storageKey: String = "HOME"
    }
    data object MyAct : FeedType {
        override val storageKey: String = "MY_ACT"
    }
    data object MyHidden : FeedType {
        override val storageKey: String = "MY_HIDDEN"
    }

    data class User(val userId: Long) : FeedType {
        override val storageKey: String = "USER_$userId"
    }
}

class PostRepository(
    private val postApiService: PostApiService,
    private val postDao: PostDao
) {
    private val _feedRefreshEvent = MutableSharedFlow<FeedType>(extraBufferCapacity = 1)
    val feedRefreshEvent = _feedRefreshEvent.asSharedFlow()

    fun getFeedStream(feedType: FeedType): Flow<List<PostResponse>> {
        return postDao.getFeed(feedType.storageKey).map { entities ->
            entities.map { it.toResponse() }
        }
    }

    suspend fun fetchFeed(feedType: FeedType, page: Int, isRefresh: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val response = when (feedType) {
                is FeedType.Home -> postApiService.getHomeFeed(page)
                is FeedType.MyAct -> postApiService.getMyActPost(page)
                is FeedType.MyHidden -> postApiService.getMyHiddenPost(page)
                is FeedType.User -> postApiService.getUserPosts(feedType.userId, page)
            }
            postDao.saveFeedPage(
                feedType = feedType.storageKey,
                posts = response.content.map { it.toEntity() },
                isRefresh = isRefresh
            )
            response.last
        }.onFailure { if (it is CancellationException) throw it }
    }

    fun getPostStream(postId: Long): Flow<PostResponse?> {
        return postDao.getPost(postId).map { it?.toResponse() }
    }

    suspend fun createPost(request: CreatePostRequest): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.createPost(request)
            _feedRefreshEvent.tryEmit(FeedType.Home)
            _feedRefreshEvent.tryEmit(FeedType.MyAct)
            Unit
        }.onFailure { if (it is CancellationException) throw it }
    }

    suspend fun editPost(postId: Long, request: EditPostRequest): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val edited = postApiService.editPost(postId, request)
            postDao.upsertPosts(listOf(edited.toEntity()))
        }.onFailure { if (it is CancellationException) throw it
        }
    }

    suspend fun deletePost(postId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.deletePost(postId)
            postDao.deletePost(postId)
        }.onFailure { if (it is CancellationException) throw it }
    }

    suspend fun hidePost(postId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching{
            postApiService.hidePost(postId)
            postDao.removePostFromFeed(FeedType.Home.storageKey, postId)
            postDao.removePostFromFeed(FeedType.MyAct.storageKey, postId)
        }.onFailure { if (it is CancellationException) throw it }
    }

    suspend fun unhidePost(postId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching{
            val updated = postApiService.unhidePost(postId)
            postDao.removePostFromFeed(FeedType.MyHidden.storageKey, postId)
            postDao.upsertPosts(listOf(updated.toEntity()))
        }.onFailure { if (it is CancellationException) throw it }
    }

    suspend fun getUserProfile(userId: Long): Result<UserProfileResponse> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getUserProfile(userId)
        }.onFailure { if (it is CancellationException) throw it }
    }

    // 게시물 수정 시
    suspend fun getPostById(postId: Long): Result<PostResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val post = postDao.getPost(postId).firstOrNull()?.toResponse()
                ?: postApiService.getPostById(postId).also {
                    postDao.upsertPosts(listOf(it.toEntity()))
                }
            post
        }.onFailure { if (it is CancellationException) throw it }
    }

    // 게시물 상세보기 시
    suspend fun getPostDetail(postId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val post = postApiService.getPostDetail(postId)
            postDao.upsertPosts(listOf(post.toEntity()))
        }.onFailure { if (it is CancellationException) throw it }
    }

    private fun PostResponse.toEntity() = PostEntity(
        id = id,
        userId = userId,
        userNickname = userNickname,
        userProfileImageUrl = userProfileImageUrl,
        title = title,
        description = description,
        videoUrl = videoUrl,
        videoThumbnailUrl = videoThumbnailUrl,
        imageUrls = imageUrls.joinToString("|||"),
        viewCount = viewCount,
        createdAt = createdAt,
        editedAt = editedAt,
        isMine = isMine,
        isHidden = isHidden,
        isUserDeleted = isUserDeleted
    )

    private fun PostEntity.toResponse() = PostResponse(
        id = id,
        userId = userId,
        userNickname = userNickname,
        userProfileImageUrl = userProfileImageUrl,
        title = title,
        description = description,
        videoUrl = videoUrl,
        videoThumbnailUrl = videoThumbnailUrl,
        imageUrls = if (imageUrls.isBlank()) emptyList() else imageUrls.split("|||"),
        viewCount = viewCount,
        createdAt = createdAt,
        editedAt = editedAt,
        isMine = isMine,
        isHidden = isHidden,
        isUserDeleted = isUserDeleted
    )
}

class UserBlockRepository(
    private val userBlockApiService: UserBlockApiService,
    private val postDao: PostDao
) {
    suspend fun blockUser(targetUserId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            userBlockApiService.blockUser(targetUserId)
            postDao.deletePostsByUserId(targetUserId)
        }.onFailure { if (it is CancellationException) throw it }
    }

    suspend fun unblockUser(targetUserId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            userBlockApiService.unblockUser(targetUserId)
        }.onFailure { if (it is CancellationException) throw it }
    }

    suspend fun getMyBlockedUser(page: Int): Result<SliceResponse<BlockedUserResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            userBlockApiService.getMyBlockedUser(page)
        }.onFailure { if (it is CancellationException) throw it }
    }
}

class ReportRepository(
    private val reportApiService: ReportApiService
) {
    suspend fun reportPost(postId: Long, reason: ReportReason, detail: String): Result<Unit> = withContext(Dispatchers.IO) {
            runCatching {
                reportApiService.reportPost(postId, CreateReportRequest(reason, detail))
            }.onFailure { if (it is CancellationException) throw it }
        }

    suspend fun reportUser(targetId: Long, reason: ReportReason, detail: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            reportApiService.reportUser(targetId, CreateReportRequest(reason, detail))
        }.onFailure { if (it is CancellationException) throw it }
    }
}

