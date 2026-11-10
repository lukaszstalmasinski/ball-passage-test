package pl.stalmasinski.ballpassage.core.calculation

/** Reasons why no ball diameter can be determined. */
public enum class CalculationProblem {
    /** 2·s ≥ d_o, so the tube has no bore. */
    WALL_TOO_THICK,

    /** d_i rounded to whole millimetres is below 20 mm. */
    INNER_DIAMETER_TOO_SMALL,

    /** d_i rounded to whole millimetres is above 57 mm. */
    INNER_DIAMETER_TOO_LARGE,

    /** r_b / d_o is below 0.5. */
    BEND_RATIO_TOO_SMALL,
}
