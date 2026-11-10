package pl.stalmasinski.ballpassage.core.balls

import pl.stalmasinski.ballpassage.core.calculation.CalculationResult

/** Picks balls from the user's set that are suitable for a calculated tube. */
public object BallSelector {
    /**
     * Returns the balls that fit [result], smallest first. The first one is the recommendation,
     * because it is the closest to the minimum diameter required by the standard.
     */
    public fun fittingBalls(result: CalculationResult.Success, balls: Collection<Ball>): List<Ball> =
        balls.filter { result.fits(it.diameter) }.sortedBy { it.diameter }
}
