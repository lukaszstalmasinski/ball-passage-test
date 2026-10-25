package pl.stalmasinski.ballpassage.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import pl.stalmasinski.ballpassage.core.Ball
import pl.stalmasinski.ballpassage.core.BallSets
import pl.stalmasinski.ballpassage.core.TubeDimensions
import java.io.File
import java.math.BigDecimal

/**
 * Runs under Robolectric so that DataStore replaces its file the way it does on Android; the plain
 * JVM fallback (File.renameTo) cannot overwrite an existing file on Windows.
 */
@RunWith(AndroidJUnit4::class)
class DataStoreUserDataRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = dataStoreScope) { File(folder.root, "test.preferences_pb") }
    }

    @After
    fun cancelDataStore() = dataStoreScope.cancel()

    private fun repository() = DataStoreUserDataRepository(dataStore)

    private var nextId = 0
    private val newId = { "id${nextId++}" }

    @Test
    fun `starts with one empty default set`() = runTest {
        val sets = repository().ballSets.first()

        assertEquals(1, sets.sets.size)
        assertEquals("", sets.active.name)
        assertEquals(emptyList<Ball>(), sets.active.balls)
    }

    @Test
    fun `ball sets survive a new repository instance`() = runTest {
        repository().updateBallSets { sets ->
            sets.addBalls(sets.activeSetId, listOf(BigDecimal("32"), BigDecimal("35")), newId)
                .let { it.updateBallDetails(it.activeSetId, it.active.balls[0].id, "K-12", "case 2") }
                .createSet("Hall A", newId())
        }

        val sets = repository().ballSets.first()

        assertEquals(listOf("", "Hall A"), sets.sets.map { it.name })
        assertEquals("Hall A", sets.active.name)
        assertEquals(Ball("id0", BigDecimal("32"), "K-12", "case 2"), sets.sets[0].balls[0])
    }

    @Test
    fun `migrates the ball list of version 1_0 with stable ids`() = runTest {
        dataStore.edit { it[stringPreferencesKey("balls")] = "35;32;abc;32.0" }
        val repository = repository()

        val migrated = repository.ballSets.first()
        assertEquals(listOf("32", "35"), migrated.active.balls.map { it.diameter.toPlainString() })
        // Ids do not change until the first write, so that the UI can refer to them.
        assertEquals(migrated, repository.ballSets.first())

        repository.updateBallSets { it.removeBall(it.activeSetId, migrated.active.balls[0].id) }

        val updated = repository.ballSets.first()
        assertEquals(listOf("35"), updated.active.balls.map { it.diameter.toPlainString() })
        assertNull(dataStore.data.first()[stringPreferencesKey("balls")])
    }

    @Test
    fun `unreadable stored sets fall back to the default`() = runTest {
        dataStore.edit { it[stringPreferencesKey("ball_sets")] = "{not json" }

        assertEquals(BallSets.initial(DataStoreUserDataRepository.DEFAULT_SET_ID), repository().ballSets.first())
    }

    @Test
    fun `history keeps the newest entries first without duplicates`() = runTest {
        val repository = repository()
        val first = tube("48.3", "5", "70")
        val second = tube("33.7", "4.5", "60")

        repository.addToHistory(first)
        repository.addToHistory(second)
        repository.addToHistory(tube("48.30", "5.0", "70"))

        assertEquals(listOf(first, second), repository.history.first())
    }

    @Test
    fun `history is limited`() = runTest {
        val repository = repository()

        repeat(UserDataRepository.MAX_HISTORY + 3) { repository.addToHistory(tube("${30 + it}", "2", "50")) }

        val history = repository.history.first()
        assertEquals(UserDataRepository.MAX_HISTORY, history.size)
        assertEquals(tube("${30 + UserDataRepository.MAX_HISTORY + 2}", "2", "50"), history.first())
    }

    @Test
    fun `clear history`() = runTest {
        val repository = repository()
        repository.addToHistory(tube("48.3", "5", "70"))

        repository.clearHistory()

        assertEquals(emptyList<TubeDimensions>(), repository.history.first())
    }

    @Test
    fun `corrupted history entries are skipped`() {
        assertEquals(
            listOf(tube("48.3", "5", "70")),
            DataStoreUserDataRepository.decodeHistory("48.3,5,70;1,2;x,y,z;0,1,1"),
        )
    }

    private fun tube(outer: String, wall: String, radius: String) =
        TubeDimensions(BigDecimal(outer), BigDecimal(wall), BigDecimal(radius))
}
