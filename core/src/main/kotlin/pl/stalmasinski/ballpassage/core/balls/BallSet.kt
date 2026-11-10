package pl.stalmasinski.ballpassage.core.balls

/** A named group of balls, e.g. one per workshop or case. An empty [name] means the default set. */
public data class BallSet(
    val id: String,
    val name: String,
    val balls: List<Ball> = emptyList(),
)
