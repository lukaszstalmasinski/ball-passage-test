package pl.stalmasinski.ballpassage.core

import java.math.BigDecimal

/** The rule that gives the test ball diameter. */
public enum class Standard {
    /** VGB-S-013-00-2014-12, section 7.2.5, Table 3. */
    VGB_S_013,

    /** KTA 3211.1 (2017-11), sections 10.2.2.2 and 10.4.2.2. */
    KTA_3211_1,
}

/** A tube as entered for one of the [Standard]s, all dimensions in millimetres. */
public sealed interface Tube {
    public val standard: Standard
    public val outerDiameter: BigDecimal
    public val wallThickness: BigDecimal

    /** Inner diameter d_i = d_o − 2·s. May be zero or negative when the wall is too thick. */
    public val innerDiameter: BigDecimal
        get() = outerDiameter - wallThickness.multiply(BigDecimal(2))
}

/**
 * The balls that are suitable for a calculated tube: at least [minBallDiameter], the diameter
 * required by the standard, and smaller than [maxBallDiameterExclusive], the inner diameter.
 */
public interface BallRequirement {
    public val minBallDiameter: BigDecimal
    public val maxBallDiameterExclusive: BigDecimal

    public fun fits(ballDiameter: BigDecimal): Boolean =
        ballDiameter >= minBallDiameter && ballDiameter < maxBallDiameterExclusive
}
