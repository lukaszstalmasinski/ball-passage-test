package pl.stalmasinski.ballpassage.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.stalmasinski.ballpassage.core.Ball
import pl.stalmasinski.ballpassage.core.BallSet
import pl.stalmasinski.ballpassage.core.BallSets
import pl.stalmasinski.ballpassage.core.TubeDimensions
import java.io.IOException

/** Data the user enters that is kept between app launches; stored only on the device. */
interface UserDataRepository {
    /** The user's ball sets; there is always at least one. */
    val ballSets: Flow<BallSets>

    /** Recently calculated tubes, newest first. */
    val history: Flow<List<TubeDimensions>>

    /** Applies [transform] to the current ball sets atomically and stores the result. */
    suspend fun updateBallSets(transform: (BallSets) -> BallSets)

    /** Adds [dimensions] to the top of the history, removing an older identical entry. */
    suspend fun addToHistory(dimensions: TubeDimensions)

    suspend fun clearHistory()

    companion object {
        const val MAX_HISTORY = 8
    }
}

class DataStoreUserDataRepository(private val dataStore: DataStore<Preferences>) : UserDataRepository {

    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    override val ballSets: Flow<BallSets> = preferences.map(::readBallSets).distinctUntilChanged()

    override val history: Flow<List<TubeDimensions>> = preferences.map { decodeHistory(it[HISTORY]) }

    override suspend fun updateBallSets(transform: (BallSets) -> BallSets) {
        dataStore.edit { prefs ->
            prefs[BALL_SETS] = encodeBallSets(transform(readBallSets(prefs)))
            prefs.remove(LEGACY_BALLS)
        }
    }

    override suspend fun addToHistory(dimensions: TubeDimensions) {
        val entry = normalize(dimensions)
        dataStore.edit { prefs ->
            val history = listOf(entry) + decodeHistory(prefs[HISTORY]).filterNot { it == entry }
            prefs[HISTORY] = encodeHistory(history.take(UserDataRepository.MAX_HISTORY))
        }
    }

    override suspend fun clearHistory() {
        dataStore.edit { it.remove(HISTORY) }
    }

    @Serializable
    internal data class StoredBallSets(val activeSetId: String, val sets: List<StoredBallSet>)

    @Serializable
    internal data class StoredBallSet(val id: String, val name: String, val balls: List<StoredBall> = emptyList())

    @Serializable
    internal data class StoredBall(val id: String, val diameter: String, val label: String = "", val location: String = "")

    internal companion object {
        private val BALL_SETS = stringPreferencesKey("ball_sets")
        private val HISTORY = stringPreferencesKey("history")

        /** Version 1.0 stored a single list of diameters, e.g. "30;32;35". */
        private val LEGACY_BALLS = stringPreferencesKey("balls")

        /** Ids are fixed for the default and migrated data, so that they are stable until the first write. */
        const val DEFAULT_SET_ID = "default"

        private const val ENTRY_SEPARATOR = ";"
        private const val VALUE_SEPARATOR = ","

        private val json = Json { ignoreUnknownKeys = true }

        fun readBallSets(prefs: Preferences): BallSets =
            prefs[BALL_SETS]?.let(::decodeBallSets)
                ?: migrateLegacyBalls(prefs[LEGACY_BALLS])

        fun migrateLegacyBalls(value: String?): BallSets {
            val balls = value.orEmpty()
                .split(ENTRY_SEPARATOR)
                .mapNotNull { it.toBigDecimalOrNull()?.takeIf { d -> d.signum() > 0 } }
                .map(BallSets::normalize)
                .distinct()
                .sorted()
                .map { Ball(id = "legacy-${it.toPlainString()}", diameter = it) }
            return BallSets(listOf(BallSet(DEFAULT_SET_ID, name = "", balls = balls)), DEFAULT_SET_ID)
        }

        fun encodeBallSets(sets: BallSets): String = json.encodeToString(
            StoredBallSets.serializer(),
            StoredBallSets(
                activeSetId = sets.activeSetId,
                sets = sets.sets.map { set ->
                    StoredBallSet(
                        id = set.id,
                        name = set.name,
                        balls = set.balls.map { StoredBall(it.id, it.diameter.toPlainString(), it.label, it.location) },
                    )
                },
            ),
        )

        /** Returns null when the stored value is unreadable, so that the defaults are used instead of crashing. */
        fun decodeBallSets(value: String): BallSets? {
            val stored = runCatching { json.decodeFromString(StoredBallSets.serializer(), value) }.getOrNull()
                ?: return null
            val sets = stored.sets.map { set ->
                BallSet(
                    id = set.id,
                    name = set.name,
                    balls = set.balls.mapNotNull { ball ->
                        val diameter = ball.diameter.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
                            ?: return@mapNotNull null
                        Ball(ball.id, diameter, ball.label, ball.location)
                    },
                )
            }
            if (sets.isEmpty()) return null
            return BallSets(sets, stored.activeSetId)
        }

        private fun normalize(dimensions: TubeDimensions) = TubeDimensions(
            outerDiameter = BallSets.normalize(dimensions.outerDiameter),
            wallThickness = BallSets.normalize(dimensions.wallThickness),
            bendRadius = BallSets.normalize(dimensions.bendRadius),
        )

        fun encodeHistory(history: List<TubeDimensions>): String =
            history.joinToString(ENTRY_SEPARATOR) { dimensions ->
                listOf(dimensions.outerDiameter, dimensions.wallThickness, dimensions.bendRadius)
                    .joinToString(VALUE_SEPARATOR) { it.toPlainString() }
            }

        // Corrupted entries are skipped rather than failing the whole list.
        fun decodeHistory(value: String?): List<TubeDimensions> =
            value.orEmpty()
                .split(ENTRY_SEPARATOR)
                .mapNotNull { entry ->
                    val numbers = entry.split(VALUE_SEPARATOR).map { it.toBigDecimalOrNull() }
                    if (numbers.size != 3 || numbers.any { it == null }) return@mapNotNull null
                    runCatching { TubeDimensions(numbers[0]!!, numbers[1]!!, numbers[2]!!) }.getOrNull()
                }
    }
}
