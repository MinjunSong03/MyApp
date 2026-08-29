package org.example.myapp.auth.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class PostApiService(
    private val client: HttpClient,
    private val baseUrl: String = "http://10.0.2.2:8081"
) {
    suspend fun createPost(token: String, request: CreatePostRequest): PostResponse {
        val response = client.post("$baseUrl/api/posts") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "게시물 생성에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }

    suspend fun getHomeFeed(token: String, page: Int, size: Int = 10): SliceResponse<PostResponse> {
        val response = client.get("$baseUrl/api/posts") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("page", page)
            parameter("size", size)
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "피드 불러오기에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }

    suspend fun getMyActPost(token: String, page: Int, size: Int = 10): SliceResponse<PostResponse> {
        val response = client.get("$baseUrl/api/posts/my_posts_act") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("page", page)
            parameter("size", size)
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "나의 활성화 게시물 불러오기에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }

    suspend fun getMyHiddenPost(token: String, page: Int, size: Int = 10): SliceResponse<PostResponse> {
        val response = client.get("$baseUrl/api/posts/my_posts_hidden") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("page", page)
            parameter("size", size)
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "나의 숨겨진 게시물 불러오기에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }

    suspend fun getPostById(token: String, postId: Long): PostResponse {
        val response = client.get("$baseUrl/api/posts/$postId/get") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "게시물 가져오기에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }

    suspend fun getPostDetail(token: String, postId: Long): PostResponse {
        val response = client.get("$baseUrl/api/posts/$postId/detail") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "상세정보 불러오기에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }

    suspend fun editPost(token: String, postId: Long, request: EditPostRequest): PostResponse {
        val response = client.patch("$baseUrl/api/posts/$postId/edit") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "게시물 업데이트에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }

    suspend fun deletePost(token: String, postId: Long) {
        val response = client.delete("$baseUrl/api/posts/$postId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "게시물 삭제에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }
    }

    suspend fun hidePost(token: String, postId: Long) {
        val response = client.post("$baseUrl/api/posts/$postId/hide") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "게시물 숨기기에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }
    }
}

class UserBlockApiService(
    private val client: HttpClient,
    private val baseUrl: String = "http://10.0.2.2:8081"
) {
    suspend fun blockUser(token: String, targetUserId: Long) {
        val response = client.post("$baseUrl/api/user/$targetUserId/block") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "사용자 차단에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }
    }


    suspend fun unblockUser(token: String, targetUserId: Long) {
        val response = client.delete("$baseUrl/api/user/$targetUserId/unblock") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "사용자 차단 해제에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }
    }
}

class ReportApiService(
    private val client: HttpClient,
    private val baseUrl: String = "http://10.0.2.2:8081"
) {
    suspend fun reportPost(token: String, postId: Long, request: CreateReportRequest) {
        val response = client.post("$baseUrl/api/posts/$postId/reports") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "게시물 신고에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }
    }
}