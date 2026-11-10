package pl.stalmasinski.ballpassage.core.calculation

import java.math.BigDecimal

/**
 * Columns of the ball diameter table, selected by the bend ratio r_b / d_o.
 *
 * Each column spans [lowerBound, upperBound); the last column has no upper bound.
 */
public enum class BendRatioColumn(
    public val lowerBound: BigDecimal,
    public val upperBound: BigDecimal?,
) {
    FROM_0_5(BigDecimal("0.5"), BigDecimal("1.0")),
    FROM_1_0(BigDecimal("1.0"), BigDecimal("1.5")),
    FROM_1_5(BigDecimal("1.5"), BigDecimal("2.0")),
    FROM_2_0(BigDecimal("2.0"), null),
    ;

    public companion object {
        /**
         * Returns the column for the given tube, or null when r_b / d_o is below 0.5.
         *
         * Bounds are compared as r_b ≥ bound·d_o so that no precision is lost by dividing.
         * A straight tube (r_b = 0) has an infinite bend ratio and falls into the last column.
         */
        public fun forTube(dimensions: TubeDimensions): BendRatioColumn? {
            if (dimensions.bendRadius.signum() == 0) return FROM_2_0
            val radius = dimensions.bendRadius
            val diameter = dimensions.outerDiameter
            return entries.firstOrNull { column ->
                radius >= column.lowerBound.multiply(diameter) &&
                    (column.upperBound == null || radius < column.upperBound.multiply(diameter))
            }
        }
    }
}
