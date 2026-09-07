package org.example.myapp.auth.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.example.myapp.auth.model.Session

const val DATASTORE_FILE_NAME = "auth_preferences.preferences_pb"
class SessionManager(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("jwt_access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("jwt_refresh_token")
        private val KEY_USER_ID = longPreferencesKey("user_id")
        private val KEY_NICKNAME = stringPreferencesKey("user_nickname")
        private val KEY_PROFILE_IMAGE = stringPreferencesKey("user_profile_image")
        private val KEY_IS_NEW_USER = booleanPreferencesKey("is_new_user")

    }

    suspend fun getAccessToken(): String? {
        return sessionFlow.firstOrNull()?.accessToken
    }

    suspend fun getSession(): Session? {
        val preferences = dataStore.data.first()
        val token = preferences[KEY_ACCESS_TOKEN] ?: return null
        return Session(
            accessToken = token,
            refreshToken = preferences[KEY_REFRESH_TOKEN],
            userId = preferences[KEY_USER_ID],
            nickname = preferences[KEY_NICKNAME],
            profileImageUrl = preferences[KEY_PROFILE_IMAGE],
            isNewUser = preferences[KEY_IS_NEW_USER] ?: false
        )
    }

    val sessionFlow: StateFlow<Session?> = dataStore.data
        .map { preferences ->
            val token = preferences[KEY_ACCESS_TOKEN] ?: return@map null
            Session(
                accessToken = token,
                refreshToken = preferences[KEY_REFRESH_TOKEN],
                userId = preferences[KEY_USER_ID],
                nickname = preferences[KEY_NICKNAME],
                profileImageUrl = preferences[KEY_PROFILE_IMAGE],
                isNewUser = preferences[KEY_IS_NEW_USER] ?: false
            )
        }
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    suspend fun saveSession(session: Session) {
        dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = session.accessToken

            if (session.refreshToken != null) {
                preferences[KEY_REFRESH_TOKEN] = session.refreshToken
            } else {
                preferences.remove(KEY_REFRESH_TOKEN)
            }

            if (session.nickname != null) {
                preferences[KEY_NICKNAME] = session.nickname
            } else {
                preferences.remove(KEY_NICKNAME)
            }

            if (session.profileImageUrl != null) {
                preferences[KEY_PROFILE_IMAGE] = session.profileImageUrl
            } else {
                preferences.remove(KEY_PROFILE_IMAGE)
            }

            if (session.userId != null) {
                preferences[KEY_USER_ID] = session.userId
            } else {
                preferences.remove(KEY_USER_ID)
            }
            preferences[KEY_IS_NEW_USER] = session.isNewUser
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}