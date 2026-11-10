package pl.stalmasinski.ballpassage.data

import kotlinx.coroutines.flow.Flow
import pl.stalmasinski.ballpassage.core.balls.BallSets
import pl.stalmasinski.ballpassage.core.calculation.TubeDimensions

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
