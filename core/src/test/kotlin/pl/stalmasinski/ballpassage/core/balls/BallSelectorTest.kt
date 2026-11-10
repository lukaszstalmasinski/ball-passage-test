package pl.stalmasinski.ballpassage.core.balls

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.stalmasinski.ballpassage.core.calculation.BallPassageCalculator
import pl.stalmasinski.ballpassage.core.calculation.CalculationResult
import pl.stalmasinski.ballpassage.core.calculation.TubeDimensions
import java.math.BigDecimal

class BallSelectorTest {

    @Test
    fun `ball selector returns fitting balls smallest first`() {
        val result = calculate("48.3", "5", "70") as CalculationResult.Success
        val balls = listOf("40", "35", "30", "32", "31.8", "38.3").map { Ball(id = it, diameter = BigDecimal(it)) }

        assertEquals(listOf("31.8", "32", "35"), BallSelector.fittingBalls(result, balls).map { it.id })
        assertEquals(emptyList<Ball>(), BallSelector.fittingBalls(result, listOf(Ball("20", BigDecimal("20")))))
    }

    private fun calculate(outer: String, wall: String, radius: String): CalculationResult =
        BallPassageCalculator.calculate(TubeDimensions(BigDecimal(outer), BigDecimal(wall), BigDecimal(radius)))
}
