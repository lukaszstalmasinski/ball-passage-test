package pl.stalmasinski.ballpassage.core.balls

import java.math.BigDecimal

/** A ball set as exchanged between devices: no ids, which are assigned on import. */
public data class SharedBallSet(
    val name: String,
    val balls: List<SharedBall>,
)

public data class SharedBall(
    val diameter: BigDecimal,
    val label: String = "",
    val location: String = "",
)
