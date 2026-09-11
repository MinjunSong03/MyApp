package org.example.myapp.auth.repository

import org.example.myapp.auth.network.CommentApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.example.myapp.auth.network.*
import kotlin.coroutines.cancellation.CancellationException

class CommentRepository(
    private val commentApiService: CommentApiService
) {
    suspend fun getComments(postId: Long, page: Int): Result<SliceResponse<CommentResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            commentApiService.getComments(postId, page)
        }.onFailure { if (it is CancellationException) throw it }
    }

    suspend fun createComment(postId: Long, content: String): Result<CommentResponse> = withContext(Dispatchers.IO) {
        runCatching {
            commentApiService.createComment(CreateCommentRequest(postId, content))
        }.onFailure { if (it is CancellationException) throw it }
    }

    suspend fun editComment(commentId: Long, content: String): Result<CommentResponse> = withContext(Dispatchers.IO) {
        runCatching {
            commentApiService.editComment(commentId, EditCommentRequest(content))
        }.onFailure { if (it is CancellationException) throw it }
    }

    suspend fun deleteComment(commentId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            commentApiService.deleteComment(commentId)
        }.onFailure { if (it is CancellationException) throw it }
    }

    suspend fun reportComment(commentId: Long, reason: ReportReason, detail: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            commentApiService.reportComment(commentId, CreateReportRequest(reason, detail))
        }.onFailure { if (it is CancellationException) throw it }
    }
}