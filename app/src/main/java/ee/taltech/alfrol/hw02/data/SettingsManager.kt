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

        val TOKEN_KEY = stringPreferencesKey("token")
        val LOGGED_IN_USER_ID_KEY = longPreferencesKey("logged_in_user_id")
        val LOCATION_UPDATE_INTERVAL_LEY = longPreferencesKey("location_update_interval")
        val LOCATION_UPDATE_FASTEST_INTERVAL_LEY =
            longPreferencesKey("location_update_fastest_interval")
        val POLYLINE_WIDTH_KEY = floatPreferencesKey("polyline_width")
        val POLYLINE_COLOR_KEY = intPreferencesKey("polyline_color")
    }

    private val datastore = context.datastore

    /**
     * Get the value from datastore.
     *
     * @param key Key to use for querying the value from the datastore.
     * @param default Default value to use in case it is not present in the datastore.
     */
    fun <T> getValue(key: Preferences.Key<T>, default: T?): Flow<T?> = datastore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[key] ?: default
        }

    /**
     * Associate a new value with the given key.
     */
    suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        datastore.edit { preferences ->
            preferences[key] = value
        }
    }
}