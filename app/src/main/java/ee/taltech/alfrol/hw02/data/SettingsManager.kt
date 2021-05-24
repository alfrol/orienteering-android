package ee.taltech.alfrol.hw02.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.datastore: DataStore<Preferences> by preferencesDataStore(SettingsManager.DATASTORE_NAME)

@Singleton
class SettingsManager @Inject constructor(@ApplicationContext context: Context) {

    companion object {
        const val DATASTORE_NAME = "gps_sport_map_settings_datastore"

        private val TOKEN_KEY = stringPreferencesKey("token")
        private val SESSION_ID_KEY = stringPreferencesKey("session_id_key")
        private val LOGGED_IN_USER_ID_KEY = longPreferencesKey("logged_in_user_id")
    }

    private val datastore = context.datastore

    /**
     * Get the token value from preferences.
     * If no token is present then returns a Flow of null.
     */
    val token: Flow<String?> = datastore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[TOKEN_KEY]
        }

    /**
     * Save the new given token to the preferences.
     */
    suspend fun saveToken(token: String) {
        datastore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    /**
     * Get the ongoing session backend id value from preferences.
     * If no id is present then returns a Flow of null.
     */
    val sessionId: Flow<String?> = datastore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SESSION_ID_KEY]
        }

    /**
     * Save the new given session id to the preferences.
     */
    suspend fun saveSessionId(id: String) {
        datastore.edit { preferences ->
            preferences[SESSION_ID_KEY] = id
        }
    }

    /**
     * Get the id of the current logged in user.
     * Can return Flow of null if there is no logged in user.
     */
    val loggedInUser: Flow<Long?> = datastore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[LOGGED_IN_USER_ID_KEY]
        }

    /**
     * Save the id of the currently logged in user.
     */
    suspend fun saveLoggedInUser(id: Long) {
        datastore.edit { preferences ->
            preferences[LOGGED_IN_USER_ID_KEY] = id
        }
    }
}