package org.example.myapp.auth.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.example.myapp.ImagePresignedRequest
import org.example.myapp.PresignedUrlResponse
import org.example.myapp.VideoPresignedRequest
import org.example.myapp.VideoPresignedUrlResponse

class MediaApiService(
    private val client: HttpClient,
    private val baseUrl: String = "http://10.0.2.2:8081"
) {
    suspend fun getImagePresignedUrl(token: String, request: ImagePresignedRequest): PresignedUrlResponse {
        val response = client.post("$baseUrl/api/media/image-presigned") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            throw IllegalStateException(errorBody?.message ?: "이미지 업로드 URL 발급에 실패했습니다.")
        }
        return response.body()
    }

    suspend fun getVideoPresignedUrl(token: String, request: VideoPresignedRequest): VideoPresignedUrlResponse {
        val response = client.post("$baseUrl/api/media/video-presigned") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            throw IllegalStateException(errorBody?.message ?: "동영상 업로드 URL 발급에 실패했습니다.")
        }
        return response.body()
    }

    suspend fun uploadBinaryToR2(uploadUrl: String, bytes: ByteArray, contentType: String) {
        val response = client.put(uploadUrl) {
            header(HttpHeaders.ContentType, contentType)
            setBody(bytes)
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("R2 스토리지 업로드 실패: HTTP ${response.status.value}")
        }
    }
}