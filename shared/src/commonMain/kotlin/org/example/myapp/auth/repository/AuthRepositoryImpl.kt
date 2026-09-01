package org.example.myapp.auth.repository

import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.example.myapp.auth.model.AuthState
import org.example.myapp.auth.model.OAuthProvider
import org.example.myapp.auth.model.Session
import org.example.myapp.auth.platform.AuthService
import org.example.myapp.auth.network.AuthApiService
import org.example.myapp.auth.local.SessionManager
class AuthRepositoryImpl(
    private val authService: AuthService,
    private val authApiService: AuthApiService,
    private val sessionManager: SessionManager
): AuthRepository {

    override val authState: StateFlow<AuthState> = sessionManager.sessionFlow
        .map { session ->
            if (session != null) {
                AuthState.Authenticated(session, session.isNewUser)
            } else {
                AuthState.Unauthenticated
            }
        }
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Eagerly,
            initialValue = AuthState.Initial
        )

    override suspend fun checkAutoLogin(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val session = sessionManager.sessionFlow.value
            if (session == null) {
                throw IllegalStateException("저장된 세션이 없습니다.")
            }
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }


    override suspend fun login(provider: OAuthProvider): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val oauthSession = authService.login(provider)
            val serverAuth = authApiService.loginWithOAuth(provider, oauthSession.accessToken)
            val session = Session(
                accessToken = serverAuth.token,
                refreshToken = null,
                userId = serverAuth.userId,
                nickname = serverAuth.nickname,
                profileImageUrl = serverAuth.profileImageUrl,
                isNewUser = serverAuth.isNewUser
            )
            sessionManager.saveSession(session)
        }.onFailure { e ->
            if (e is CancellationException) throw e
            sessionManager.clearSession()
        }
    }

    override suspend fun logout(provider: OAuthProvider?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (provider != null) {
                runCatching {
                    authService.logout(provider)
                }.onFailure { e ->
                    println("SDK Logout Failure: ${e.message}")
                }
            }
            sessionManager.clearSession()
        }.onFailure { e ->
            if (e is CancellationException) throw e
            sessionManager.clearSession()
        }
    }


    override suspend fun unlink(provider: OAuthProvider): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val session = sessionManager.sessionFlow.value
                ?: throw IllegalStateException("유효하지 않은 Access Token입니다.")
            authApiService.unlinkAccount(provider, session.accessToken)
            sessionManager.clearSession()
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }

    override suspend fun updateNickname(nickname: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val session = sessionManager.sessionFlow.value
                ?: throw IllegalStateException("로그인 세션이 만료되었습니다.")

            authApiService.updateNickname(token = session.accessToken, nickname = nickname)
            val updatedSession = session.copy(nickname = nickname, isNewUser = false)
            sessionManager.saveSession(updatedSession)
        }.onFailure { e ->
            if (e is CancellationException) throw e
        }
    }
}