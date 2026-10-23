package pl.stalmasinski.ballpassage.core

import java.math.BigDecimal
import java.math.RoundingMode

/** Bend ratio r_b / d_o. */
public sealed interface BendRatio {
    /** A straight tube (r_b = 0), treated as an infinite bend ratio. */
    public data object StraightTube : BendRatio

    /**
     * Finite bend ratio, truncated to [DISPLAY_SCALE] decimal places.
     *
     * Truncation (not rounding) keeps the shown value inside the same table column,
     * because all column bounds are multiples of 0.5.
     */
    public data class Finite(val value: BigDecimal) : BendRatio

    public companion object {
        public const val DISPLAY_SCALE: Int = 2

        public fun of(dimensions: TubeDimensions): BendRatio =
            if (dimensions.bendRadius.signum() == 0) {
                StraightTube
            } else {
                Finite(dimensions.bendRadius.divide(dimensions.outerDiameter, DISPLAY_SCALE, RoundingMode.DOWN))
            }
    }
}

/** Reasons why no ball diameter can be determined. */
public enum class CalculationProblem {
    /** 2·s ≥ d_o, so the tube has no bore. */
    WALL_TOO_THICK,

    /** d_i rounded to whole millimetres is outside 20–57 mm. */
    INNER_DIAMETER_OUT_OF_RANGE,

    /** r_b / d_o is below 0.5. */
    BEND_RATIO_TOO_SMALL,
}

public sealed interface CalculationResult {
    public val dimensions: TubeDimensions
    public val innerDiameter: BigDecimal get() = dimensions.innerDiameter
    public val bendRatio: BendRatio get() = BendRatio.of(dimensions)

    public data class Success(
        override val dimensions: TubeDimensions,
        val row: DiameterRow,
        val column: BendRatioColumn,
        val deduction: BigDecimal,
        val ballDiameter: BigDecimal,
    ) : CalculationResult {
        /** True when the result comes from a row that VGB-S-013 does not cover. */
        val isExtrapolated: Boolean get() = !row.coveredByStandard
    }

    public data class Failure(
        override val dimensions: TubeDimensions,
        val problems: Set<CalculationProblem>,
    ) : CalculationResult
}

/** Calculates the ball diameter for the ball passage test (Kugeldurchlaufversuch). */
public object BallPassageCalculator {
    public fun calculate(dimensions: TubeDimensions): CalculationResult {
        val innerDiameter = dimensions.innerDiameter
        if (innerDiameter.signum() <= 0) {
            return CalculationResult.Failure(dimensions, setOf(CalculationProblem.WALL_TOO_THICK))
        }

        val row = DiameterRow.forInnerDiameter(innerDiameter)
        val column = BendRatioColumn.forTube(dimensions)
        if (row == null || column == null) {
            val problems = buildSet {
                if (row == null) add(CalculationProblem.INNER_DIAMETER_OUT_OF_RANGE)
                if (column == null) add(CalculationProblem.BEND_RATIO_TOO_SMALL)
            }
            return CalculationResult.Failure(dimensions, problems)
        }

        val deduction = BallPassageTable.deduction(row, column)
        return CalculationResult.Success(
            dimensions = dimensions,
            row = row,
            column = column,
            deduction = deduction,
            ballDiameter = innerDiameter - deduction,
        )
    }
}
