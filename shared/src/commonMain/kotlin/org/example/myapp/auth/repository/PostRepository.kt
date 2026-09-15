package org.example.myapp.auth.repository

import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.example.myapp.auth.local.SessionManager
import org.example.myapp.auth.network.*

class PostRepository(
    private val postApiService: PostApiService,
    private val sessionManager: SessionManager
) {
    private val mutex = Mutex()
    private val _homePosts = MutableStateFlow<List<PostResponse>>(emptyList())
    val homePosts: StateFlow<List<PostResponse>> = _homePosts.asStateFlow()

    private val _myActPosts = MutableStateFlow<List<PostResponse>>(emptyList())
    val myActPosts: StateFlow<List<PostResponse>> = _myActPosts.asStateFlow()

    private val _myHiddenPosts = MutableStateFlow<List<PostResponse>>(emptyList())
    val myHiddenPosts: StateFlow<List<PostResponse>> = _myHiddenPosts.asStateFlow()

    private fun insertSorted(list: List<PostResponse>, newItem: PostResponse): List<PostResponse> {
        val mutable = list.toMutableList()
        mutable.removeAll { it.id == newItem.id }
        val targetIndex = mutable.indexOfFirst { it.id < newItem.id }
        if (targetIndex != -1) {
            mutable.add(targetIndex, newItem)
        } else {
            mutable.add(newItem)
        }
        return mutable
    }

    suspend fun createPost(request: CreatePostRequest): Result<PostResponse> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.createPost(request)
        }.onSuccess { newPost ->
            mutex.withLock {
                _homePosts.value = insertSorted(_homePosts.value, newPost)
                _myActPosts.value = insertSorted(_myActPosts.value, newPost)
            }
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }
    suspend fun getHomeFeed(page: Int, isRefresh: Boolean): Result<SliceResponse<PostResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getHomeFeed(page)
        }.onSuccess { slice ->
            mutex.withLock {
                if (isRefresh) {
                    _homePosts.value = slice.content
                } else {
                    _homePosts.value = (_homePosts.value + slice.content).distinctBy { it.id }
                }
            }
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun getMyActPost(page: Int, isRefresh: Boolean): Result<SliceResponse<PostResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getMyActPost(page)
        }.onSuccess { slice ->
            mutex.withLock {
                if (isRefresh) {
                    _myActPosts.value = slice.content
                } else {
                    _myActPosts.value = (_myActPosts.value + slice.content).distinctBy { it.id }
                }
            }
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    fun removePostsByUserId(userId: Long) {
        _homePosts.value = _homePosts.value.filterNot { it.userId == userId }
    }

    suspend fun getMyHiddenPost(page: Int, isRefresh: Boolean): Result<SliceResponse<PostResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getMyHiddenPost(page)
        }.onSuccess { slice ->
            mutex.withLock {
                if (isRefresh) {
                    _myHiddenPosts.value = slice.content
                } else {
                    _myHiddenPosts.value = (_myHiddenPosts.value + slice.content).distinctBy { it.id }
                }
            }
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun getUserPosts(userId: Long, page: Int): Result<SliceResponse<PostResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getUserPosts(userId, page)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun getUserProfile(userId: Long): Result<UserProfileResponse> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.getUserProfile(userId)
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
            mutex.withLock {
                _homePosts.value = _homePosts.value.map { if (it.id == postId) editedPost else it }
                _myActPosts.value = _myActPosts.value.map { if (it.id == postId) editedPost else it }
                _myHiddenPosts.value = _myHiddenPosts.value.map { if (it.id == postId) editedPost else it }
            }
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun deletePost(postId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postApiService.deletePost(postId)
        }.onSuccess {
            mutex.withLock {
                _homePosts.value = _homePosts.value.filterNot { it.id == postId }
                _myActPosts.value = _myActPosts.value.filterNot { it.id == postId }
                _myHiddenPosts.value = _myHiddenPosts.value.filterNot { it.id == postId }
            }
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun hidePost(postId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching{
            postApiService.hidePost(postId)
        }.onSuccess {
            mutex.withLock {
                val currentUserId = sessionManager.getSession()?.userId

                val target = _homePosts.value.firstOrNull { it.id == postId }
                    ?: _myActPosts.value.firstOrNull { it.id == postId }


                _homePosts.value = _homePosts.value.filterNot { it.id == postId }
                _myActPosts.value = _myActPosts.value.filterNot { it.id == postId }

                if (target != null && target.userId == currentUserId) {
                    val hiddenItem = target.copy(isHidden = true)
                    _myHiddenPosts.value = insertSorted(_myHiddenPosts.value, hiddenItem)
                }
            }
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    suspend fun unhidePost(post: PostResponse): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching{
            postApiService.unhidePost(post.id)
        }.onSuccess {
            mutex.withLock {
                val unhiddenItem = post.copy(isHidden = false)
                _myHiddenPosts.value = _myHiddenPosts.value.filterNot { it.id == post.id }
                _myActPosts.value = insertSorted(_myActPosts.value, unhiddenItem)
                if (_homePosts.value.isNotEmpty()) {
                    _homePosts.value = insertSorted(_homePosts.value, unhiddenItem)
                }
            }
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

    suspend fun reportUser(targetId: Long, reason: ReportReason, detail: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            reportApiService.reportUser(targetId, CreateReportRequest(reason, detail))
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }
}

