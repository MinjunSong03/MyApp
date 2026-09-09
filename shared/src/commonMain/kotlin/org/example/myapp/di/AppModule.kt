package org.example.myapp.di

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.myapp.auth.local.SessionManager
import org.example.myapp.auth.repository.AuthRepository
import org.example.myapp.auth.repository.AuthRepositoryImpl
import org.example.myapp.auth.viewmodel.AppViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.example.myapp.auth.network.AuthApiService
import org.example.myapp.auth.network.ErrorResponse
import org.example.myapp.auth.network.MediaApiService
import org.example.myapp.auth.network.PostApiService
import org.example.myapp.auth.network.RefreshTokenRequest
import org.example.myapp.auth.network.ReportApiService
import org.example.myapp.auth.network.TokenRefreshResponse
import org.example.myapp.auth.network.UserBlockApiService
import org.example.myapp.auth.repository.MediaRepository
import org.example.myapp.auth.repository.PostRepository
import org.example.myapp.auth.repository.ReportRepository
import org.example.myapp.auth.repository.UserBlockRepository
import org.example.myapp.auth.viewmodel.CreatePostViewModel
import org.example.myapp.auth.viewmodel.EditProfileViewModel
import org.example.myapp.auth.viewmodel.EditPostViewModel
import org.example.myapp.auth.viewmodel.ManageMyViewModel
import org.example.myapp.auth.viewmodel.MyPostViewModel
import org.example.myapp.auth.viewmodel.HomeViewModel
import org.example.myapp.auth.viewmodel.LoginViewModel
import org.example.myapp.auth.viewmodel.MyInfoViewModel
import org.example.myapp.auth.viewmodel.PostDetailViewModel
import org.example.myapp.auth.viewmodel.ProfileClickUiState
import org.example.myapp.auth.viewmodel.ProfileClickViewModel
import org.example.myapp.auth.viewmodel.ProfileSetupViewModel
import org.koin.core.module.dsl.viewModel

val commonModule = module {
    single {
        val sessionManager: SessionManager = get()
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }

            install(Auth) {
                bearer {
                    sendWithoutRequest { request ->
                        !request.url.host.contains("cloudflarestorage.com") &&
                                !request.url.host.contains("amazonaws.com")
                    }

                    loadTokens {
                        val session = sessionManager.getSession()
                        if (session?.accessToken != null && session.refreshToken != null) {
                            BearerTokens(session.accessToken, session.refreshToken)
                        } else null
                    }

                    refreshTokens {
                        val currentSession = sessionManager.getSession()
                        val refreshToken = currentSession?.refreshToken ?: return@refreshTokens null

                        val refreshResult = runCatching {
                            client.post("http://localhost:8081/api/auth/refresh") {
                                markAsRefreshTokenRequest()
                                contentType(ContentType.Application.Json)
                                setBody(RefreshTokenRequest(refreshToken))
                            }.body<TokenRefreshResponse>()
                        }.getOrNull()

                        if (refreshResult != null) {
                            val updatedSession = currentSession.copy(
                                accessToken = refreshResult.accessToken,
                                refreshToken = refreshResult.refreshToken
                            )
                            sessionManager.saveSession(updatedSession)
                            BearerTokens(refreshResult.accessToken, refreshResult.refreshToken)
                        } else {
                            sessionManager.clearSession()
                            null
                        }
                    }
                }
            }

            HttpResponseValidator {
                validateResponse { response ->
                    val isUnauthorized = response.status == HttpStatusCode.Unauthorized
                    val isInvalidUser = !response.status.isSuccess() && runCatching {
                        response.body<ErrorResponse>()
                    }.getOrNull()?.message?.contains("Invalid user", ignoreCase = true) == true

                    if (isUnauthorized || isInvalidUser) {
                        sessionManager.clearSession()
                        throw IllegalStateException()
                    }
                }
            }
        }
    }

    single { AuthApiService(get(), "http://localhost:8081") }
    single { PostApiService(get(),"http://localhost:8081") }
    single { UserBlockApiService(get()) }
    single { ReportApiService(get()) }
    single { MediaApiService(get(), "http://localhost:8081") }

    single { SessionManager(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    single { PostRepository(get()) }
    single { UserBlockRepository(get()) }
    single { ReportRepository(get()) }
    single { MediaRepository(get()) }

    viewModel { AppViewModel(get()) }
    viewModel { CreatePostViewModel(get(), get()) }
    viewModel { EditProfileViewModel(get(), get()) }
    viewModel { EditPostViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { ManageMyViewModel(get()) }
    viewModel { MyInfoViewModel(get()) }
    viewModel { MyPostViewModel(get(), get(), get()) }
    viewModel { ProfileSetupViewModel(get(), get()) }
    viewModel { PostDetailViewModel(get(), get(),get(), get()) }
    viewModel { ProfileClickViewModel(get(), get(), get()) }
}

expect val platformModule: Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(commonModule, platformModule)
    }
}