package org.example.myapp.auth

import org.example.myapp.auth.model.OAuthProvider
import org.example.myapp.auth.model.Session
import org.example.myapp.auth.platform.AuthService

class IosAuthService: AuthService {
    override suspend fun login(provider: OAuthProvider): Session {
        TODO("Not yet implemented")
    }

    override suspend fun logout(provider: OAuthProvider) {
        TODO("Not yet implemented")
    }

    override suspend fun unlink(provider: OAuthProvider) {
        TODO("Not yet implemented")
    }
}