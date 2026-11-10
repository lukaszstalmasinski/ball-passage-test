package pl.stalmasinski.ballpassage.core.calculation

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
