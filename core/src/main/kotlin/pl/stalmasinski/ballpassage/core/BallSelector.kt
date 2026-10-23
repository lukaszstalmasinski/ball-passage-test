package pl.stalmasinski.ballpassage.core

import java.math.BigDecimal

/** Picks balls from the user's set that are suitable for a calculated tube. */
public object BallSelector {
    /**
     * Returns the balls that fit [result], smallest first. The first one is the recommendation,
     * because it is the closest to the minimum diameter required by the standard.
     */
    public fun fittingBalls(result: CalculationResult.Success, balls: Collection<BigDecimal>): List<BigDecimal> =
        balls.filter(result::fits).sorted()
}
