package pl.stalmasinski.ballpassage.core

import java.math.BigDecimal

/**
 * A cold-bent tube for the ball passage test according to KTA 3211.1.
 *
 * @property undersize largest permissible undersize ("größte zulässige Maßunterschreitung") in
 *   millimetres. KTA 3211.1 does not define it further; it comes from the tube specification.
 */
public data class KtaTube(
    override val outerDiameter: BigDecimal,
    override val wallThickness: BigDecimal,
    val undersize: BigDecimal,
) : Tube {
    init {
        require(outerDiameter.signum() > 0) { "Outer diameter must be greater than zero." }
        require(wallThickness.signum() > 0) { "Wall thickness must be greater than zero." }
        require(undersize.signum() >= 0) { "Undersize must not be negative." }
    }

    override val standard: Standard get() = Standard.KTA_3211_1
}

/** Tubes covered by KTA 3211.1: seamless cold-bent heat exchanger tubes up to these sizes. */
public enum class KtaTubeClass(public val maxOuterDiameter: BigDecimal, public val maxWallThickness: BigDecimal) {
    /** Section 10.2: ferritic steel, d_o ≤ 38 mm, s ≤ 4 mm. */
    FERRITIC(BigDecimal("38"), BigDecimal("4")),

    /** Section 10.4: austenitic steel, d_o ≤ 42.4 mm, s ≤ 3.6 mm. */
    AUSTENITIC(BigDecimal("42.4"), BigDecimal("3.6")),
    ;

    public fun covers(tube: Tube): Boolean =
        tube.outerDiameter <= maxOuterDiameter && tube.wallThickness <= maxWallThickness
}

public enum class KtaProblem {
    /** 2·s ≥ d_o, so the tube has no bore. */
    WALL_TOO_THICK,

    /** The deductions are at least as large as d_i, so no ball diameter results. */
    NO_BALL,
}

public sealed interface KtaResult {
    public val tube: KtaTube
    public val innerDiameter: BigDecimal get() = tube.innerDiameter

    /** The 0.1·d_i term of the formula. */
    public val tenthOfInnerDiameter: BigDecimal get() = innerDiameter.multiply(KtaCalculator.TENTH)

    public data class Success(
        override val tube: KtaTube,
        override val minBallDiameter: BigDecimal,
    ) : KtaResult, BallRequirement {
        override val maxBallDiameterExclusive: BigDecimal get() = innerDiameter

        /** True when the tube is larger than the tubes KTA 3211.1 covers (sections 10.2 and 10.4). */
        val isOutsideScope: Boolean get() = KtaTubeClass.entries.none { it.covers(tube) }
    }

    public data class Failure(override val tube: KtaTube, val problem: KtaProblem) : KtaResult
}

/**
 * KTA 3211.1 (2017-11), 10.2.2.2 (3) and 10.4.2.2 (3): a ball of diameter
 * D_K = D_i − (größte zulässige Maßunterschreitung + 0,1·D_i + 2 mm) must pass freely,
 * where D_i is the nominal inner diameter of the tube.
 */
public object KtaCalculator {
    public val TENTH: BigDecimal = BigDecimal("0.1")
    public val FIXED_ALLOWANCE: BigDecimal = BigDecimal("2")

    public fun calculate(tube: KtaTube): KtaResult {
        val innerDiameter = tube.innerDiameter
        if (innerDiameter.signum() <= 0) return KtaResult.Failure(tube, KtaProblem.WALL_TOO_THICK)

        val ball = innerDiameter - (tube.undersize + innerDiameter.multiply(TENTH) + FIXED_ALLOWANCE)
        if (ball.signum() <= 0) return KtaResult.Failure(tube, KtaProblem.NO_BALL)
        return KtaResult.Success(tube, ball)
    }
}
