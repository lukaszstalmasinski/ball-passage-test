package pl.stalmasinski.ballpassage.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.stalmasinski.ballpassage.core.TubeDimensions
import java.math.BigDecimal

class FakeUserDataRepository(
    balls: List<BigDecimal> = emptyList(),
    history: List<TubeDimensions> = emptyList(),
) : UserDataRepository {
    private val ballsState = MutableStateFlow(balls.sorted())
    private val historyState = MutableStateFlow(history)

    override val balls: StateFlow<List<BigDecimal>> = ballsState.asStateFlow()
    override val history: StateFlow<List<TubeDimensions>> = historyState.asStateFlow()

    override suspend fun addBall(diameter: BigDecimal) {
        if (ballsState.value.none { it.compareTo(diameter) == 0 }) ballsState.value = (ballsState.value + diameter).sorted()
    }

    override suspend fun removeBall(diameter: BigDecimal) {
        ballsState.value = ballsState.value.filterNot { it.compareTo(diameter) == 0 }
    }

    override suspend fun addToHistory(dimensions: TubeDimensions) {
        historyState.value = (listOf(dimensions) + historyState.value.filterNot { it == dimensions })
            .take(UserDataRepository.MAX_HISTORY)
    }

    override suspend fun clearHistory() {
        historyState.value = emptyList()
    }
}
