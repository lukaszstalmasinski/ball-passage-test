package pl.stalmasinski.ballpassage.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import pl.stalmasinski.ballpassage.core.TubeDimensions
import java.io.IOException
import java.math.BigDecimal

/** Data the user enters that is kept between app launches; stored only on the device. */
interface UserDataRepository {
    /** Ball diameters available in the user's workshop, smallest first. */
    val balls: Flow<List<BigDecimal>>

    /** Recently calculated tubes, newest first. */
    val history: Flow<List<TubeDimensions>>

    suspend fun addBall(diameter: BigDecimal)

    suspend fun removeBall(diameter: BigDecimal)

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

    override val balls: Flow<List<BigDecimal>> = preferences.map { decodeBalls(it[BALLS]) }

    override val history: Flow<List<TubeDimensions>> = preferences.map { decodeHistory(it[HISTORY]) }

    override suspend fun addBall(diameter: BigDecimal) {
        dataStore.edit { prefs ->
            val balls = decodeBalls(prefs[BALLS])
            if (balls.none { it.compareTo(diameter) == 0 }) {
                prefs[BALLS] = encodeBalls(balls + diameter)
            }
        }
    }

    override suspend fun removeBall(diameter: BigDecimal) {
        dataStore.edit { prefs ->
            prefs[BALLS] = encodeBalls(decodeBalls(prefs[BALLS]).filterNot { it.compareTo(diameter) == 0 })
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

    internal companion object {
        private val BALLS = stringPreferencesKey("balls")
        private val HISTORY = stringPreferencesKey("history")

        private const val ENTRY_SEPARATOR = ";"
        private const val VALUE_SEPARATOR = ","

        /** 32.0 and 32 are the same ball; stores numbers without trailing zeros or exponent. */
        fun normalize(value: BigDecimal): BigDecimal =
            value.stripTrailingZeros().let { if (it.scale() < 0) it.setScale(0) else it }

        private fun normalize(dimensions: TubeDimensions) = TubeDimensions(
            outerDiameter = normalize(dimensions.outerDiameter),
            wallThickness = normalize(dimensions.wallThickness),
            bendRadius = normalize(dimensions.bendRadius),
        )

        fun encodeBalls(balls: List<BigDecimal>): String =
            balls.map(::normalize).sorted().joinToString(ENTRY_SEPARATOR) { it.toPlainString() }

        // Corrupted entries are skipped rather than failing the whole list.
        fun decodeBalls(value: String?): List<BigDecimal> =
            value.orEmpty()
                .split(ENTRY_SEPARATOR)
                .mapNotNull { it.toBigDecimalOrNull()?.takeIf { d -> d.signum() > 0 } }
                .sorted()

        fun encodeHistory(history: List<TubeDimensions>): String =
            history.joinToString(ENTRY_SEPARATOR) { dimensions ->
                listOf(dimensions.outerDiameter, dimensions.wallThickness, dimensions.bendRadius)
                    .joinToString(VALUE_SEPARATOR) { it.toPlainString() }
            }

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
