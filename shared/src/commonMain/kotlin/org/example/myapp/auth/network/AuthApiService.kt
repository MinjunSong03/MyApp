package org.example.myapp.auth.network

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.call.body
import org.example.myapp.auth.model.OAuthProvider
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

class AuthApiService(
    private val client: HttpClient,
    private val baseUrl: String = "http://10.0.2.2:8081"

) {
    suspend fun loginWithOAuth(provider: OAuthProvider, token: String): AuthResponse {
        val endpoint = when (provider) {
            OAuthProvider.KAKAO -> "kakao"
            OAuthProvider.NAVER -> "naver"
            OAuthProvider.GOOGLE -> "google"
            OAuthProvider.APPLE -> "apple"
        }

        val response = client.post("$baseUrl/api/auth/$endpoint/login") {
            contentType(ContentType.Application.Json)
            setBody(OAuthLoginRequest(accessToken = token))
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "로그인에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }

        return response.body()
    }

    suspend fun unlinkAccount(provider: OAuthProvider, token: String) {
        val endpoint = when (provider) {
            OAuthProvider.KAKAO -> "kakao"
            OAuthProvider.NAVER -> "naver"
            OAuthProvider.GOOGLE -> "google"
            OAuthProvider.APPLE -> "apple"
        }

        val response = client.post("$baseUrl/api/auth/$endpoint/unlink") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "회원 탈퇴에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }
    }

    suspend fun updateNickname(token: String, nickname: String) {
        val response = client.patch("$baseUrl/api/user/updateNickname") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(UpdateNicknameRequest(nickname = nickname))
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
            val message = errorBody?.message ?: "닉네임 변경에 실패했습니다. (${response.status.value})"
            throw IllegalStateException(message)
        }
    }
}