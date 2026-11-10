package pl.stalmasinski.ballpassage.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.stalmasinski.ballpassage.core.BallSets
import pl.stalmasinski.ballpassage.core.Standard
import pl.stalmasinski.ballpassage.core.Tube

class FakeUserDataRepository(
    ballSets: BallSets = BallSets.initial("default"),
    history: List<Tube> = emptyList(),
    standard: Standard = Standard.VGB_S_013,
) : UserDataRepository {
    private val ballSetsState = MutableStateFlow(ballSets)
    private val historyState = MutableStateFlow(history)
    private val standardState = MutableStateFlow(standard)

    override val ballSets: StateFlow<BallSets> = ballSetsState.asStateFlow()
    override val history: StateFlow<List<Tube>> = historyState.asStateFlow()
    override val standard: StateFlow<Standard> = standardState.asStateFlow()

    override suspend fun updateBallSets(transform: (BallSets) -> BallSets) {
        ballSetsState.value = transform(ballSetsState.value)
    }

    override suspend fun addToHistory(tube: Tube) {
        historyState.value = (listOf(tube) + historyState.value.filterNot { it == tube })
            .take(UserDataRepository.MAX_HISTORY)
    }

    override suspend fun clearHistory() {
        historyState.value = emptyList()
    }

    override suspend fun setStandard(standard: Standard) {
        standardState.value = standard
    }
}
