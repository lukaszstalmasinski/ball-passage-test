package pl.stalmasinski.ballpassage.core.calculation

import java.math.BigDecimal

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
                if (row == null) {
                    add(
                        if (innerDiameter < BigDecimal(DiameterRow.entries.first().min)) {
                            CalculationProblem.INNER_DIAMETER_TOO_SMALL
                        } else {
                            CalculationProblem.INNER_DIAMETER_TOO_LARGE
                        },
                    )
                }
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
            minBallDiameter = innerDiameter - deduction,
        )
    }
}
