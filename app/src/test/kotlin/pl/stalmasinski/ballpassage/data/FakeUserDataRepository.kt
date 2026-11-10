package pl.stalmasinski.ballpassage.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.stalmasinski.ballpassage.core.balls.BallSets
import pl.stalmasinski.ballpassage.core.calculation.TubeDimensions
import java.io.IOException

class FakeUserDataRepository(
    ballSets: BallSets = BallSets.initial("default"),
    history: List<TubeDimensions> = emptyList(),
) : UserDataRepository {
    /** When true, every write fails like a full disk does with DataStore. */
    var failWrites = false

    private val ballSetsState = MutableStateFlow(ballSets)
    private val historyState = MutableStateFlow(history)

    override val ballSets: StateFlow<BallSets> = ballSetsState.asStateFlow()
    override val history: StateFlow<List<TubeDimensions>> = historyState.asStateFlow()

    override suspend fun updateBallSets(transform: (BallSets) -> BallSets) {
        checkWritable()
        ballSetsState.value = transform(ballSetsState.value)
    }

    override suspend fun addToHistory(dimensions: TubeDimensions) {
        checkWritable()
        historyState.value = (listOf(dimensions) + historyState.value.filterNot { it == dimensions })
            .take(UserDataRepository.MAX_HISTORY)
    }

    override suspend fun clearHistory() {
        checkWritable()
        historyState.value = emptyList()
    }

    private fun checkWritable() {
        if (failWrites) throw IOException("No space left on device")
    }
}
