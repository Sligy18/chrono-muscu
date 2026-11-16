package fr.gillesray.chronomuscu

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    val presets: List<Int> = Presets.DEFAULTS,
    val sound: EndSound = EndSound.BIP,
    val vibrate: Boolean = true,
    val duck: Boolean = true,
    val series: Int = 0,
)

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private fun presetKey(index: Int) = intPreferencesKey("preset_$index")
    private val soundKey = stringPreferencesKey("sound")
    private val vibrateKey = booleanPreferencesKey("vibrate")
    private val duckKey = booleanPreferencesKey("duck")
    private val seriesKey = intPreferencesKey("series")

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            presets = List(Presets.COUNT) { i -> p[presetKey(i)] ?: Presets.DEFAULTS[i] },
            sound = p[soundKey]?.let { name -> EndSound.entries.firstOrNull { it.name == name } }
                ?: EndSound.BIP,
            vibrate = p[vibrateKey] ?: true,
            duck = p[duckKey] ?: true,
            series = p[seriesKey] ?: 0,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setPreset(index: Int, seconds: Int) {
        store.edit { it[presetKey(index)] = Presets.clamp(seconds) }
    }

    suspend fun setSound(sound: EndSound) {
        store.edit { it[soundKey] = sound.name }
    }

    suspend fun setVibrate(on: Boolean) {
        store.edit { it[vibrateKey] = on }
    }

    suspend fun setDuck(on: Boolean) {
        store.edit { it[duckKey] = on }
    }

    suspend fun adjustSeries(delta: Int) {
        store.edit { it[seriesKey] = Series.adjust(it[seriesKey] ?: 0, delta) }
    }

    suspend fun resetSeries() {
        store.edit { it[seriesKey] = 0 }
    }
}
