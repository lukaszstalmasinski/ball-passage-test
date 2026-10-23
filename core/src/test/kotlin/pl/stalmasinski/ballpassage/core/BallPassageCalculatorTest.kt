package pl.stalmasinski.ballpassage.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

class BallPassageCalculatorTest {

    /** Worked examples from the hand calculations made for the original Kulowanie app. */
    @ParameterizedTest(name = "d_o={0}, s={1}, r_b={2} -> ball {3}")
    @CsvSource(
        "48.3, 5,   70,  31.8",
        "57,   6,   120, 39",
        "33.7, 4.5, 60,  19.7",
        "26.3, 2,   20,  16.3",
    )
    fun `reference examples`(outer: String, wall: String, radius: String, expectedBall: String) {
        val result = calculate(outer, wall, radius)

        assertInstanceOf(CalculationResult.Success::class.java, result)
        assertNumberEquals(expectedBall, (result as CalculationResult.Success).ballDiameter)
    }

    @Test
    fun `success exposes intermediate values`() {
        val result = calculate("48.3", "5", "70") as CalculationResult.Success

        assertNumberEquals("38.3", result.innerDiameter)
        assertEquals(BendRatio.Finite(BigDecimal("1.44")), result.bendRatio)
        assertEquals(DiameterRow.D30_39, result.row)
        assertEquals(BendRatioColumn.FROM_1_0, result.column)
        assertNumberEquals("6.5", result.deduction)
        assertFalse(result.isExtrapolated)
    }

    /** Every cell of Table 3, using d_o = 100 so that r_b equals the bend ratio times 100. */
    @ParameterizedTest(name = "d_i={0}, r_b/d_o={1} -> deduction {2}")
    @CsvSource(
        "25, 0.5, 6", "25, 1.0, 5.5", "25, 1.5, 5", "25, 2.0, 4",
        "35, 0.5, 7", "35, 1.0, 6.5", "35, 1.5, 6", "35, 2.0, 5",
        "45, 0.5, 8", "45, 1.0, 7.5", "45, 1.5, 7", "45, 2.0, 6",
        "55, 0.5, 9", "55, 1.0, 8.5", "55, 1.5, 8", "55, 2.0, 7",
    )
    fun `table cells`(innerDiameter: String, ratio: String, expectedDeduction: String) {
        val outer = BigDecimal(100)
        val wall = (outer - BigDecimal(innerDiameter)).divide(BigDecimal(2))
        val radius = BigDecimal(ratio).multiply(outer)

        val result = BallPassageCalculator.calculate(TubeDimensions(outer, wall, radius)) as CalculationResult.Success

        assertNumberEquals(expectedDeduction, result.deduction)
        assertNumberEquals(BigDecimal(innerDiameter) - BigDecimal(expectedDeduction), result.ballDiameter)
    }

    @ParameterizedTest(name = "r_b={0} with d_o=40 -> {1}")
    @CsvSource(
        "19.99, ",
        "20,    FROM_0_5",
        "39.99, FROM_0_5",
        "40,    FROM_1_0",
        "59.99, FROM_1_0",
        "60,    FROM_1_5",
        "79.99, FROM_1_5",
        "80,    FROM_2_0",
        "1000,  FROM_2_0",
        "0,     FROM_2_0",
    )
    fun `bend ratio column bounds`(radius: String, expected: BendRatioColumn?) {
        val dimensions = TubeDimensions(BigDecimal(40), BigDecimal(5), BigDecimal(radius))

        assertEquals(expected, BendRatioColumn.forTube(dimensions))
    }

    @Test
    fun `bend ratio bounds are exact for non-terminating ratios`() {
        // 16.85 / 33.7 is exactly 0.5; a floating point division could land just below it.
        val dimensions = TubeDimensions(BigDecimal("33.7"), BigDecimal("4.5"), BigDecimal("16.85"))

        assertEquals(BendRatioColumn.FROM_0_5, BendRatioColumn.forTube(dimensions))
    }

    @ParameterizedTest(name = "d_i={0} -> {1}")
    @CsvSource(
        "19.49, ",
        "19.5,  D20_29",
        "29.49, D20_29",
        "29.5,  D30_39",
        "39.5,  D40_49",
        "49.49, D40_49",
        "49.5,  D50_57",
        "57.49, D50_57",
        "57.5,  ",
    )
    fun `inner diameter rows use rounding to whole millimetres`(innerDiameter: String, expected: DiameterRow?) {
        assertEquals(expected, DiameterRow.forInnerDiameter(BigDecimal(innerDiameter)))
    }

    @Test
    fun `rows above 49 mm are extrapolated`() {
        val result = calculate("63", "4", "200") as CalculationResult.Success

        assertEquals(DiameterRow.D50_57, result.row)
        assertTrue(result.isExtrapolated)
        assertNumberEquals("48", result.ballDiameter)
    }

    @Test
    fun `straight tube uses the last column`() {
        val result = calculate("48.3", "5", "0") as CalculationResult.Success

        assertEquals(BendRatio.StraightTube, result.bendRatio)
        assertEquals(BendRatioColumn.FROM_2_0, result.column)
        assertNumberEquals("33.3", result.ballDiameter)
    }

    @Test
    fun `bend ratio is truncated for display`() {
        val result = calculate("33.7", "4.5", "60")

        // 60 / 33.7 = 1.7804...
        assertEquals(BendRatio.Finite(BigDecimal("1.78")), result.bendRatio)
    }

    @Test
    fun `wall too thick`() {
        val result = calculate("20", "10", "40") as CalculationResult.Failure

        assertEquals(setOf(CalculationProblem.WALL_TOO_THICK), result.problems)
    }

    @Test
    fun `inner diameter out of range`() {
        val result = calculate("20", "2", "40") as CalculationResult.Failure

        assertEquals(setOf(CalculationProblem.INNER_DIAMETER_OUT_OF_RANGE), result.problems)
        assertNumberEquals("16", result.innerDiameter)
    }

    @Test
    fun `bend ratio too small`() {
        val result = calculate("48.3", "5", "20") as CalculationResult.Failure

        assertEquals(setOf(CalculationProblem.BEND_RATIO_TOO_SMALL), result.problems)
    }

    @Test
    fun `reports all range problems at once`() {
        val result = calculate("100", "5", "10") as CalculationResult.Failure

        assertEquals(
            setOf(CalculationProblem.INNER_DIAMETER_OUT_OF_RANGE, CalculationProblem.BEND_RATIO_TOO_SMALL),
            result.problems,
        )
    }

    @Test
    fun `rejects invalid dimensions`() {
        assertThrows<IllegalArgumentException> { calculate("0", "1", "10") }
        assertThrows<IllegalArgumentException> { calculate("10", "0", "10") }
        assertThrows<IllegalArgumentException> { calculate("10", "1", "-1") }
    }

    private fun calculate(outer: String, wall: String, radius: String): CalculationResult =
        BallPassageCalculator.calculate(TubeDimensions(BigDecimal(outer), BigDecimal(wall), BigDecimal(radius)))

    private fun assertNumberEquals(expected: String, actual: BigDecimal) =
        assertNumberEquals(BigDecimal(expected), actual)

    private fun assertNumberEquals(expected: BigDecimal, actual: BigDecimal) =
        assertEquals(0, expected.compareTo(actual), "expected $expected but was $actual")
}
