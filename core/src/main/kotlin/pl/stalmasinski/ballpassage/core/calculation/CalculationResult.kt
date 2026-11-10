package pl.stalmasinski.ballpassage.core.calculation

import java.math.BigDecimal

public sealed interface CalculationResult {
    public val dimensions: TubeDimensions
    public val innerDiameter: BigDecimal get() = dimensions.innerDiameter
    public val bendRatio: BendRatio get() = BendRatio.of(dimensions)

    /**
     * The ball may be smaller than d_i by at most [deduction] millimetres, so any ball with
     * [minBallDiameter] ≤ diameter < d_i is suitable for the test.
     */
    public data class Success(
        override val dimensions: TubeDimensions,
        val row: DiameterRow,
        val column: BendRatioColumn,
        val deduction: BigDecimal,
        val minBallDiameter: BigDecimal,
    ) : CalculationResult {
        /** True when the result comes from a row that VGB-S-013 does not cover. */
        val isExtrapolated: Boolean get() = !row.coveredByStandard

        /** Upper limit (exclusive) for the ball diameter: the inner diameter of the tube. */
        val maxBallDiameterExclusive: BigDecimal get() = innerDiameter

        public fun fits(ballDiameter: BigDecimal): Boolean =
            ballDiameter >= minBallDiameter && ballDiameter < maxBallDiameterExclusive
    }

    public data class Failure(
        override val dimensions: TubeDimensions,
        val problems: Set<CalculationProblem>,
    ) : CalculationResult {
        /** Smallest bend radius for which Table 3 applies to this tube: 0.5·d_o. */
        val minBendRadius: BigDecimal
            get() = BendRatioColumn.entries.first().lowerBound.multiply(dimensions.outerDiameter)
    }
}
