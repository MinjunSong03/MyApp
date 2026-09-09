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
    private val baseUrl: String
) {
    suspend fun createPost(request: CreatePostRequest): PostResponse {
        val response = client.post("$baseUrl/api/posts") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "게시물 생성에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }

    suspend fun getHomeFeed(page: Int, size: Int = 10): SliceResponse<PostResponse> {
        val response = client.get("$baseUrl/api/posts") {
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

    suspend fun getMyActPost(page: Int, size: Int = 10): SliceResponse<PostResponse> {
        val response = client.get("$baseUrl/api/posts/my_posts_act") {
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

    suspend fun getUserPosts(userId: Long, page: Int, size: Int = 10): SliceResponse<PostResponse> {
        val response = client.get("$baseUrl/api/posts/user/$userId") {
            parameter("page", page)
            parameter("size", size)
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "사용자의 게시물 불러오기에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }

    suspend fun getUserProfile(userId: Long): UserProfileResponse {
        val response = client.get("$baseUrl/api/user/$userId")
        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            throw IllegalStateException(errorBody?.message ?: "사용자 정보를 불러오지 못했습니다.")
        }
        return response.body()
    }

    suspend fun getMyHiddenPost(page: Int, size: Int = 10): SliceResponse<PostResponse> {
        val response = client.get("$baseUrl/api/posts/my_posts_hidden") {
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

    suspend fun getPostById(postId: Long): PostResponse {
        val response = client.get("$baseUrl/api/posts/$postId/get") {
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "게시물 가져오기에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }

    suspend fun getPostDetail(postId: Long): PostResponse {
        val response = client.get("$baseUrl/api/posts/$postId/detail") {
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "상세정보 불러오기에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }

    suspend fun editPost(postId: Long, request: EditPostRequest): PostResponse {
        val response = client.patch("$baseUrl/api/posts/$postId/edit") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "게시물 업데이트에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }

    suspend fun deletePost(postId: Long) {
        val response = client.delete("$baseUrl/api/posts/$postId") {
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "게시물 삭제에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }
    }

    suspend fun hidePost(postId: Long) {
        val response = client.post("$baseUrl/api/posts/$postId/hide") {
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "게시물 숨기기에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }
    }

    suspend fun unhidePost(postId: Long) {
        val response = client.delete("$baseUrl/api/posts/$postId/unhide") {
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "게시물 숨기기 해제에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }
    }
}

class UserBlockApiService(
    private val client: HttpClient,
    private val baseUrl: String = "http://localhost:8081"
) {
    suspend fun blockUser(targetUserId: Long) {
        val response = client.post("$baseUrl/api/user/$targetUserId/block") {
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "사용자 차단에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }
    }

    suspend fun unblockUser(targetUserId: Long) {
        val response = client.delete("$baseUrl/api/user/$targetUserId/unblock") {
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "사용자 차단 해제에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }
    }

    suspend fun getMyBlockedUser(page: Int, size: Int = 10): SliceResponse<BlockedUserResponse> {
        val response = client.get("$baseUrl/api/user/my_blocked_user") {
            parameter("page", page)
            parameter("size", size)
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "차단한 사용자 불러오기에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }
}

class ReportApiService(
    private val client: HttpClient,
    private val baseUrl: String = "http://localhost:8081"
) {
    suspend fun reportPost(postId: Long, request: CreateReportRequest) {
        val response = client.post("$baseUrl/api/posts/$postId/reports") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "게시물 신고에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }
    }
}