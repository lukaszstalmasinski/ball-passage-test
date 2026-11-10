package pl.stalmasinski.ballpassage.core.balls

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.stalmasinski.ballpassage.core.ball
import pl.stalmasinski.ballpassage.core.calculate
import pl.stalmasinski.ballpassage.core.calculation.CalculationResult

class BallSelectorTest {

    @Test
    fun `ball selector returns fitting balls smallest first`() {
        val result = calculate("48.3", "5", "70") as CalculationResult.Success
        val balls = listOf("40", "35", "30", "32", "31.8", "38.3").map { ball(it) }

        assertEquals(listOf("31.8", "32", "35"), BallSelector.fittingBalls(result, balls).map { it.id })
        assertEquals(emptyList<Ball>(), BallSelector.fittingBalls(result, listOf(ball("20"))))
    }

}
