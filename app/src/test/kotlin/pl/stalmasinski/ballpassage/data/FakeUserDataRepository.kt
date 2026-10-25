package pl.stalmasinski.ballpassage.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.stalmasinski.ballpassage.core.BallSets
import pl.stalmasinski.ballpassage.core.TubeDimensions

class FakeUserDataRepository(
    ballSets: BallSets = BallSets.initial("default"),
    history: List<TubeDimensions> = emptyList(),
) : UserDataRepository {
    private val ballSetsState = MutableStateFlow(ballSets)
    private val historyState = MutableStateFlow(history)

    override val ballSets: StateFlow<BallSets> = ballSetsState.asStateFlow()
    override val history: StateFlow<List<TubeDimensions>> = historyState.asStateFlow()

    override suspend fun updateBallSets(transform: (BallSets) -> BallSets) {
        ballSetsState.value = transform(ballSetsState.value)
    }

    override suspend fun addToHistory(dimensions: TubeDimensions) {
        historyState.value = (listOf(dimensions) + historyState.value.filterNot { it == dimensions })
            .take(UserDataRepository.MAX_HISTORY)
    }

    override suspend fun clearHistory() {
        historyState.value = emptyList()
    }
}
