package pl.stalmasinski.ballpassage.core

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Rows of the ball diameter table, selected by the inner diameter d_i rounded to whole millimetres.
 *
 * @property coveredByStandard false for rows that are not part of VGB-S-013 Table 3 and were extrapolated.
 */
public enum class DiameterRow(
    public val min: Int,
    public val max: Int,
    public val coveredByStandard: Boolean,
) {
    D20_29(20, 29, coveredByStandard = true),
    D30_39(30, 39, coveredByStandard = true),
    D40_49(40, 49, coveredByStandard = true),
    D50_57(50, 57, coveredByStandard = false),
    ;

    public companion object {
        /** Returns the row for [innerDiameter], or null when it is outside 20–57 mm after rounding. */
        public fun forInnerDiameter(innerDiameter: BigDecimal): DiameterRow? {
            val rounded = innerDiameter.setScale(0, RoundingMode.HALF_UP)
            return entries.firstOrNull {
                rounded >= BigDecimal(it.min) && rounded <= BigDecimal(it.max)
            }
        }
    }
}

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

/**
 * VGB-S-013-00-2014-12, section 7.2.5, Table 3 (Kugeldurchlaufversuch):
 * by how many millimetres the ball diameter must be smaller than the inner diameter of the tube.
 */
public object BallPassageTable {
    private val deductions: Map<DiameterRow, List<BigDecimal>> = mapOf(
        DiameterRow.D20_29 to listOf("6", "5.5", "5", "4"),
        DiameterRow.D30_39 to listOf("7", "6.5", "6", "5"),
        DiameterRow.D40_49 to listOf("8", "7.5", "7", "6"),
        DiameterRow.D50_57 to listOf("9", "8.5", "8", "7"),
    ).mapValues { (_, values) -> values.map(::BigDecimal) }

    public fun deduction(row: DiameterRow, column: BendRatioColumn): BigDecimal =
        deductions.getValue(row)[column.ordinal]
}
