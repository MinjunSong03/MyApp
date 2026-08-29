package org.example.myapp.auth.platform

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import org.example.myapp.auth.model.OAuthProvider
import org.example.myapp.auth.model.Session
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.util.Log

private const val TAG = "KakaoSDK"

class AndroidAuthService(private val context: Context): AuthService {
    override suspend fun login(provider: OAuthProvider): Session {
        return when (provider) {
            OAuthProvider.KAKAO -> loginWithKakao()
            else -> throw IllegalArgumentException("지원하지 않는 로그인 제공자입니다: $provider")
        }
    }

    override suspend fun logout(provider: OAuthProvider) {
        when (provider) {
            OAuthProvider.KAKAO -> logoutFromKakao()
            else -> throw IllegalArgumentException("지원하지 않는 로그아웃 제공자입니다: $provider")
        }
    }

    override suspend fun unlink(provider: OAuthProvider) {
        when (provider) {
            OAuthProvider.KAKAO -> unlinkFromKakao()
            else -> throw IllegalArgumentException("지원하지 않는 회원탈퇴 제공자입니다: $provider")
        }
    }

    //Kakao Login, Logout, Unlink
    private suspend fun loginWithKakao(): Session = suspendCancellableCoroutine { continuation ->
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                continuation.resumeWithException(error)
            } else if (token != null) {
                val session = Session(
                    accessToken = token.accessToken,
                    refreshToken = token.refreshToken,
                    userId = null,
                    nickname = null,
                    profileImageUrl = null
                )
                continuation.resume(session)
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakao(context, callback = callback)
        } else {
            UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
        }
    }

    private suspend fun logoutFromKakao() = suspendCancellableCoroutine { continuation ->
        UserApiClient.instance.logout { error ->
            if (error != null) {
                continuation.resumeWithException(error)
            } else {
                continuation.resume(Unit)
            }
        }
    }

    private suspend fun unlinkFromKakao() = suspendCancellableCoroutine { continuation ->
        UserApiClient.instance.unlink { error ->
            if (error != null) {
                Log.w(TAG, "SDK Unlink Failure: ${error.message}")
            } else {
                Log.w(TAG, "SDK Unlink Success")
            }
            continuation.resume(Unit)
        }
    }
}