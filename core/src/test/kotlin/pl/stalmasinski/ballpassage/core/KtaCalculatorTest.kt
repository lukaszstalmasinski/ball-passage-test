package pl.stalmasinski.ballpassage.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

class KtaCalculatorTest {

    @ParameterizedTest(name = "{0} × {1}, undersize {2} → {3}")
    @CsvSource(
        // D_K = D_i − (undersize + 0.1·D_i + 2 mm) = 0.9·D_i − undersize − 2
        "25, 2.6, 0.5, 15.32",
        "25, 2.6, 0, 15.82",
        "38, 4, 0.3, 24.7",
        "42.4, 3.6, 0.4, 29.28",
        "20, 2, 1, 11.4",
    )
    fun `ball diameter`(outer: String, wall: String, undersize: String, expected: String) {
        val result = KtaCalculator.calculate(tube(outer, wall, undersize)) as KtaResult.Success

        assertNumberEquals(expected, result.minBallDiameter)
    }

    @Test
    fun `steps of the calculation`() {
        val result = KtaCalculator.calculate(tube("25", "2.6", "0.5")) as KtaResult.Success

        assertNumberEquals("19.8", result.innerDiameter)
        assertNumberEquals("1.98", result.tenthOfInnerDiameter)
        assertNumberEquals("19.8", result.maxBallDiameterExclusive)
    }

    @Test
    fun `fitting balls are at least D_K and smaller than the inner diameter`() {
        val result = KtaCalculator.calculate(tube("25", "2.6", "0.5")) as KtaResult.Success

        assertFalse(result.fits(BigDecimal("15.3")))
        assertTrue(result.fits(BigDecimal("15.32")))
        assertTrue(result.fits(BigDecimal("19.7")))
        assertFalse(result.fits(BigDecimal("19.8")))
        assertEquals(
            listOf("16", "18"),
            BallSelector.fittingBalls(result, listOf("20", "18", "15", "16").map { Ball(it, BigDecimal(it)) }).map { it.id },
        )
    }

    @Test
    fun `scope of sections 10_2 and 10_4`() {
        fun outside(outer: String, wall: String) =
            (KtaCalculator.calculate(tube(outer, wall, "0")) as KtaResult.Success).isOutsideScope

        assertFalse(outside("38", "4"))       // ferritic limit
        assertFalse(outside("42.4", "3.6"))   // austenitic limit
        assertFalse(outside("40", "3"))       // austenitic only
        assertTrue(outside("40", "4"))        // too thick for austenitic, too large for ferritic
        assertTrue(outside("48.3", "3"))
        assertTrue(outside("30", "4.5"))
    }

    @Test
    fun `too thick wall and too small tube are failures`() {
        assertEquals(KtaProblem.WALL_TOO_THICK, (KtaCalculator.calculate(tube("10", "5", "0")) as KtaResult.Failure).problem)
        // D_i = 2 mm: 0.9 · 2 − 2 < 0
        assertEquals(KtaProblem.NO_BALL, (KtaCalculator.calculate(tube("6", "2", "0")) as KtaResult.Failure).problem)
        // D_i = 20 mm: 0.9 · 20 − 2 − 16 = 0
        assertEquals(KtaProblem.NO_BALL, (KtaCalculator.calculate(tube("24", "2", "16")) as KtaResult.Failure).problem)
    }

    @Test
    fun `invalid tubes are rejected`() {
        assertThrows<IllegalArgumentException> { tube("0", "2", "0") }
        assertThrows<IllegalArgumentException> { tube("25", "0", "0") }
        assertThrows<IllegalArgumentException> { tube("25", "2", "-0.1") }
    }

    @Test
    fun `tubes know their standard`() {
        assertEquals(Standard.KTA_3211_1, tube("25", "2", "0").standard)
        assertEquals(Standard.VGB_S_013, TubeDimensions(BigDecimal("25"), BigDecimal("2"), BigDecimal("50")).standard)
    }

    private fun tube(outer: String, wall: String, undersize: String) =
        KtaTube(BigDecimal(outer), BigDecimal(wall), BigDecimal(undersize))

    private fun assertNumberEquals(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual), "expected $expected but was $actual")
}
