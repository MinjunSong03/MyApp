package org.example.myapp.auth.platform

import org.example.myapp.auth.model.OAuthProvider
import org.example.myapp.auth.model.Session

interface AuthService {
    suspend fun login(provider: OAuthProvider): Session
    suspend fun logout(provider: OAuthProvider)
    suspend fun unlink(provider: OAuthProvider)
}