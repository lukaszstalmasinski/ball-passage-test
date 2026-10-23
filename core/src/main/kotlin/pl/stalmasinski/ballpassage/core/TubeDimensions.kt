package pl.stalmasinski.ballpassage.core

import java.math.BigDecimal

/**
 * Dimensions of a bent tube, all in millimetres.
 *
 * @property outerDiameter outer diameter of the tube (d_o), must be greater than zero.
 * @property wallThickness wall thickness of the tube (s), must be greater than zero.
 * @property bendRadius mean bend radius (r_b), zero means a straight tube.
 */
public data class TubeDimensions(
    val outerDiameter: BigDecimal,
    val wallThickness: BigDecimal,
    val bendRadius: BigDecimal,
) {
    init {
        require(outerDiameter.signum() > 0) { "Outer diameter must be greater than zero." }
        require(wallThickness.signum() > 0) { "Wall thickness must be greater than zero." }
        require(bendRadius.signum() >= 0) { "Bend radius must not be negative." }
    }

    /** Inner diameter d_i = d_o − 2·s. May be zero or negative when the wall is too thick. */
    val innerDiameter: BigDecimal
        get() = outerDiameter - wallThickness.multiply(TWO)

    private companion object {
        val TWO: BigDecimal = BigDecimal(2)
    }
}
