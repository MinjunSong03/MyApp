package org.example.myapp.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import org.example.myapp.auth.network.PostApiService
import org.example.myapp.auth.network.ReportApiService
import org.example.myapp.auth.network.UserBlockApiService
import org.example.myapp.auth.repository.PostRepository
import org.example.myapp.auth.repository.ReportRepository
import org.example.myapp.auth.repository.UserBlockRepository
import org.example.myapp.auth.viewmodel.CreatePostViewModel
import org.example.myapp.auth.viewmodel.DetailViewModel
import org.example.myapp.auth.viewmodel.EditPostViewModel
import org.example.myapp.auth.viewmodel.ManageMyViewModel
import org.example.myapp.auth.viewmodel.MyPostViewModel
import org.example.myapp.auth.viewmodel.HomeScreenViewModel
import org.example.myapp.auth.viewmodel.LoginViewModel
import org.example.myapp.auth.viewmodel.MyInfoViewModel
import org.example.myapp.auth.viewmodel.ProfileSetupViewModel
import org.koin.core.module.dsl.viewModel

val commonModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    single { AuthApiService(get()) }
    single { PostApiService(get()) }
    single { UserBlockApiService(get()) }
    single { ReportApiService(get()) }

    single { SessionManager(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    single { PostRepository(get(), get()) }
    single { UserBlockRepository(get(), get()) }
    single { ReportRepository(get(), get()) }

    viewModel { AppViewModel(get()) }
    viewModel { CreatePostViewModel(get()) }
    viewModel { DetailViewModel(get()) }
    viewModel { EditPostViewModel(get()) }
    viewModel { HomeScreenViewModel(get(), get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { ManageMyViewModel(get()) }
    viewModel { MyInfoViewModel(get()) }
    viewModel { MyPostViewModel(get(), get(), get()) }
    viewModel { ProfileSetupViewModel(get()) }
}

expect val platformModule: Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(commonModule, platformModule)
    }
}