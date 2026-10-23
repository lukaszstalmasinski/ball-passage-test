package pl.stalmasinski.ballpassage.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
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

    @After
    fun cancelDataStore() = dataStoreScope.cancel()

    private fun repository() = DataStoreUserDataRepository(
        PreferenceDataStoreFactory.create(scope = dataStoreScope) { File(folder.root, "test.preferences_pb") },
    )

    @Test
    fun `balls are sorted, normalized and deduplicated`() = runTest {
        val repository = repository()

        repository.addBall(BigDecimal("35"))
        repository.addBall(BigDecimal("31.80"))
        repository.addBall(BigDecimal("35.0"))

        assertEquals(listOf(BigDecimal("31.8"), BigDecimal("35")), repository.balls.first())
    }

    @Test
    fun `removing a ball ignores the scale`() = runTest {
        val repository = repository()
        repository.addBall(BigDecimal("32"))

        repository.removeBall(BigDecimal("32.00"))

        assertEquals(emptyList<BigDecimal>(), repository.balls.first())
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
    fun `corrupted entries are skipped`() {
        assertEquals(
            listOf(BigDecimal("12"), BigDecimal("32")),
            DataStoreUserDataRepository.decodeBalls("32;abc;;-1;0;12"),
        )
        assertEquals(
            listOf(tube("48.3", "5", "70")),
            DataStoreUserDataRepository.decodeHistory("48.3,5,70;1,2;x,y,z;0,1,1"),
        )
    }

    private fun tube(outer: String, wall: String, radius: String) =
        TubeDimensions(BigDecimal(outer), BigDecimal(wall), BigDecimal(radius))
}
