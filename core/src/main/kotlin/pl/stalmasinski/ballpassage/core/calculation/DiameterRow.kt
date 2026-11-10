package pl.stalmasinski.ballpassage.core.calculation

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
