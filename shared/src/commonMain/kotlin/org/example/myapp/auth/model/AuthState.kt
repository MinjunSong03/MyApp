package org.example.myapp.auth.model

sealed class AuthState {
    object Initial: AuthState()
    data class Loading(val message: String? = null): AuthState()
    object Unauthenticated: AuthState()
    data class Authenticated(val session: Session, val isNewUser: Boolean = false): AuthState()
}