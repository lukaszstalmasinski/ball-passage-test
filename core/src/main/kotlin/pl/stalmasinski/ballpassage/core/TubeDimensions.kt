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
    override val outerDiameter: BigDecimal,
    override val wallThickness: BigDecimal,
    val bendRadius: BigDecimal,
) : Tube {
    init {
        require(outerDiameter.signum() > 0) { "Outer diameter must be greater than zero." }
        require(wallThickness.signum() > 0) { "Wall thickness must be greater than zero." }
        require(bendRadius.signum() >= 0) { "Bend radius must not be negative." }
    }

    override val standard: Standard get() = Standard.VGB_S_013
}
