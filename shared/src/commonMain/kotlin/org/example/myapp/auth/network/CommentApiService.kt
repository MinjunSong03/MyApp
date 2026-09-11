package org.example.myapp.auth.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class CommentApiService(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun createComment(request: CreateCommentRequest): CommentResponse {
        val response = client.post("$baseUrl/api/comments") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            val error = runCatching { response.body<ErrorResponse>() }.getOrNull()
            throw IllegalStateException(error?.message ?: "댓글 작성에 실패했습니다.")
        }
        return response.body()
    }

    suspend fun getComments(postId: Long, page: Int, size: Int = 15): SliceResponse<CommentResponse> {
        val response = client.get("$baseUrl/api/comments") {
            parameter("postId", postId)
            parameter("page", page)
            parameter("size", size)
        }
        if (!response.status.isSuccess()) {
            val error = runCatching { response.body<ErrorResponse>() }.getOrNull()
            throw IllegalStateException(error?.message ?: "댓글을 불러오지 못했습니다.")
        }
        return response.body()
    }

    suspend fun editComment(commentId: Long, request: EditCommentRequest): CommentResponse {
        val response = client.patch("$baseUrl/api/comments/$commentId") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            val error = runCatching { response.body<ErrorResponse>() }.getOrNull()
            throw IllegalStateException(error?.message ?: "댓글 수정에 실패했습니다.")
        }
        return response.body()
    }

    suspend fun deleteComment(commentId: Long) {
        val response = client.delete("$baseUrl/api/comments/$commentId")
        if (!response.status.isSuccess()) {
            val error = runCatching { response.body<ErrorResponse>() }.getOrNull()
            throw IllegalStateException(error?.message ?: "댓글 삭제에 실패했습니다.")
        }
    }

    suspend fun reportComment(commentId: Long, request: CreateReportRequest) {
        val response = client.post("$baseUrl/api/comments/$commentId/reports") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            val error = runCatching { response.body<ErrorResponse>() }.getOrNull()
            throw IllegalStateException(error?.message ?: "댓글 신고에 실패했습니다.")
        }
    }
}