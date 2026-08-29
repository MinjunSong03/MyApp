package org.example.myapp.auth.repository

import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.example.myapp.auth.local.SessionManager
import org.example.myapp.auth.network.*

class PostRepository(
    private val postApiService: PostApiService,
    private val sessionManager: SessionManager
) {
    private fun getAccessToken(): String =
        sessionManager.sessionFlow.value?.accessToken ?: throw IllegalStateException("로그인이 필요합니다.")
    suspend fun createPost(request: CreatePostRequest): Result<PostResponse> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.createPost(getAccessToken(), request)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }
    suspend fun getHomeFeed(page: Int): Result<SliceResponse<PostResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getHomeFeed(getAccessToken(), page)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun getMyActPost(page: Int): Result<SliceResponse<PostResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getMyActPost(getAccessToken(), page)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun getMyHiddenPost(page: Int): Result<SliceResponse<PostResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getMyHiddenPost(getAccessToken(), page)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun getPostById(postId: Long): Result<PostResponse> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getPostById(getAccessToken(), postId)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun getPostDetail(postId: Long): Result<PostResponse> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getPostDetail(getAccessToken(), postId)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun editPost(postId: Long, request: EditPostRequest): Result<PostResponse> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.editPost(getAccessToken(), postId, request)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun deletePost(postId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.deletePost(getAccessToken(), postId)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun hidePost(postId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching{
            postApiService.hidePost(getAccessToken(), postId)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }
}

class UserBlockRepository(
    private val userBlockApiService: UserBlockApiService,
    private val sessionManager: SessionManager
) {
    private suspend fun getAccessToken(): String =
        sessionManager.sessionFlow.value?.accessToken ?: throw IllegalStateException("로그인이 필요합니다.")

    suspend fun blockUser(targetUserId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            userBlockApiService.blockUser(getAccessToken(), targetUserId)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun unblockUser(targetUserId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            userBlockApiService.unblockUser(getAccessToken(), targetUserId)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }
}

class ReportRepository(
    private val reportApiService: ReportApiService,
    private val sessionManager: SessionManager
) {
    private suspend fun getAccessToken(): String =
        sessionManager.sessionFlow.value?.accessToken ?: throw IllegalStateException("로그인이 필요합니다.")

    suspend fun reportPost(postId: Long, reason: ReportReason, detail: String): Result<Unit> = withContext(Dispatchers.IO) {
            runCatching {
                reportApiService.reportPost(getAccessToken(), postId, CreateReportRequest(reason, detail))
            }.onFailure { e ->
                if (e is CancellationException) throw e
            }
        }
}

