package org.example.myapp.auth.repository

import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.example.myapp.auth.local.SessionManager
import org.example.myapp.auth.network.*

class PostRepository(
    private val postApiService: PostApiService
) {
    private val _postEditedEvent = MutableSharedFlow<PostResponse>()
    val postEditedEvent: SharedFlow<PostResponse> = _postEditedEvent.asSharedFlow()

    private val _createPostEvent = MutableSharedFlow<PostResponse>()
    val createPostEvent: SharedFlow<PostResponse> = _createPostEvent.asSharedFlow()

    private val _postHiddenEvent = MutableSharedFlow<Long>()
    val postHiddenEvent: SharedFlow<Long> = _postHiddenEvent.asSharedFlow()

    private val _postUnhiddenEvent = MutableSharedFlow<PostResponse>()
    val postUnhiddenEvent: SharedFlow<PostResponse> = _postUnhiddenEvent.asSharedFlow()

    private val _postDeletedEvent = MutableSharedFlow<Long>()
    val postDeletedEvent: SharedFlow<Long> = _postDeletedEvent.asSharedFlow()

    suspend fun createPost(request: CreatePostRequest): Result<PostResponse> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.createPost(request)
        }.onSuccess { createPost ->
            _createPostEvent.emit(createPost)

        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }
    suspend fun getHomeFeed(page: Int): Result<SliceResponse<PostResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getHomeFeed(page)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun getMyActPost(page: Int): Result<SliceResponse<PostResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getMyActPost(page)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun getMyHiddenPost(page: Int): Result<SliceResponse<PostResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getMyHiddenPost(page)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun getPostById(postId: Long): Result<PostResponse> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getPostById(postId)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun getPostDetail(postId: Long): Result<PostResponse> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getPostDetail(postId)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun editPost(postId: Long, request: EditPostRequest): Result<PostResponse> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.editPost(postId, request)
        }.onSuccess { editedPost ->
            _postEditedEvent.emit(editedPost)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun deletePost(postId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.deletePost(postId)
        }.onSuccess {
            _postDeletedEvent.emit(postId)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun hidePost(postId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching{
            postApiService.hidePost(postId)
        }.onSuccess {
            _postHiddenEvent.emit(postId)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun unhidePost(post: PostResponse): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching{
            postApiService.unhidePost(post.id)
        }.onSuccess {
            _postUnhiddenEvent.emit(post.copy(isHidden = false))
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }
}

class UserBlockRepository(
    private val userBlockApiService: UserBlockApiService
) {
    suspend fun blockUser(targetUserId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            userBlockApiService.blockUser(targetUserId)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun unblockUser(targetUserId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            userBlockApiService.unblockUser(targetUserId)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun getMyBlockedUser(page: Int): Result<SliceResponse<BlockedUserResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            userBlockApiService.getMyBlockedUser(page)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }
}

class ReportRepository(
    private val reportApiService: ReportApiService
) {
    suspend fun reportPost(postId: Long, reason: ReportReason, detail: String): Result<Unit> = withContext(Dispatchers.IO) {
            runCatching {
                reportApiService.reportPost(postId, CreateReportRequest(reason, detail))
            }.onFailure { e ->
                if (e is CancellationException) throw e
            }
        }
}

