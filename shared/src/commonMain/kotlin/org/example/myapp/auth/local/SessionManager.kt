package org.example.myapp.auth.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.example.myapp.auth.model.Session

const val DATASTORE_FILE_NAME = "auth_preferences.preferences_pb"
class SessionManager(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("jwt_access_token")
        private val KEY_USER_ID = longPreferencesKey("user_id")
        private val KEY_NICKNAME = stringPreferencesKey("user_nickname")
        private val KEY_PROFILE_IMAGE = stringPreferencesKey("user_profile_image")
        private val KEY_IS_NEW_USER = booleanPreferencesKey("is_new_user")

    }

    val sessionFlow: StateFlow<Session?> = dataStore.data
        .map { preferences ->
            val token = preferences[KEY_ACCESS_TOKEN] ?: return@map null
            Session(
                accessToken = token,
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
            session.userId?.let { preferences[KEY_USER_ID] = it }
            session.nickname?.let { preferences[KEY_NICKNAME] = it }
            session.profileImageUrl?.let { preferences[KEY_PROFILE_IMAGE] = it }
            preferences[KEY_IS_NEW_USER] = session.isNewUser
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}