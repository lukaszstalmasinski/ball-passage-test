package pl.stalmasinski.ballpassage.core

import java.math.BigDecimal

/** Diameters from [from] to [to] (inclusive) every [step], for adding a whole range of balls at once. */
public object BallRange {
    public const val MAX_COUNT: Int = 200

    public sealed interface Result {
        public data class Valid(val diameters: List<BigDecimal>) : Result

        public data class Invalid(val error: Error) : Result
    }

    public enum class Error {
        FROM_NOT_POSITIVE,
        STEP_NOT_POSITIVE,
        TO_BELOW_FROM,
        TOO_MANY,
    }

    public fun of(from: BigDecimal, to: BigDecimal, step: BigDecimal): Result {
        val error = when {
            from.signum() <= 0 -> Error.FROM_NOT_POSITIVE
            step.signum() <= 0 -> Error.STEP_NOT_POSITIVE
            to < from -> Error.TO_BELOW_FROM
            (to - from).divideToIntegralValue(step) >= BigDecimal(MAX_COUNT) -> Error.TOO_MANY
            else -> null
        }
        if (error != null) return Result.Invalid(error)

        val diameters = generateSequence(from) { it + step }
            .takeWhile { it <= to }
            .map(BallSets::normalize)
            .toList()
        return Result.Valid(diameters)
    }
}
