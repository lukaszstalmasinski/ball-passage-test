package pl.stalmasinski.ballpassage.core.balls

import java.math.BigDecimal

/**
 * A steel ball available in the workshop.
 *
 * @property label marking used to identify the ball, e.g. "K-12"; empty when not set.
 * @property location where the ball is kept, e.g. "case 2"; empty when not set.
 */
public data class Ball(
    val id: String,
    val diameter: BigDecimal,
    val label: String = "",
    val location: String = "",
)
