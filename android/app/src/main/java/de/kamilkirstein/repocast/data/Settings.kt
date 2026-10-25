package de.kamilkirstein.repocast.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

/**
 * Local app settings and listening progress (Jetpack DataStore).
 * The token can only create issues in the RepoCast repo, so app-private storage is enough.
 */
class Settings(context: Context) {
    private val store = context.applicationContext.dataStore
    private val tokenKey = stringPreferencesKey("github_token")
    private val speedKey = floatPreferencesKey("speed")

    val token: Flow<String> = store.data.map { it[tokenKey].orEmpty() }
    val speed: Flow<Float> = store.data.map { it[speedKey] ?: 1f }

    /** Progress per episode, keyed by its audio path. */
    val progress: Flow<Map<String, EpisodeProgress>> = store.data.map { prefs ->
        prefs.asMap().keys.map { it.name }
            .filter { it.startsWith("pos:") }
            .associate { key ->
                val id = key.removePrefix("pos:")
                id to EpisodeProgress(
                    positionMs = prefs[longPreferencesKey(key)] ?: 0,
                    durationMs = prefs[longPreferencesKey("dur:$id")] ?: 0,
                )
            }
    }

    suspend fun setToken(token: String) = store.edit { it[tokenKey] = token.trim() }

    suspend fun setSpeed(speed: Float) = store.edit { it[speedKey] = speed }

    suspend fun saveProgress(audio: String, positionMs: Long, durationMs: Long) = store.edit {
        it[longPreferencesKey("pos:$audio")] = positionMs
        it[longPreferencesKey("dur:$audio")] = durationMs
    }
}
