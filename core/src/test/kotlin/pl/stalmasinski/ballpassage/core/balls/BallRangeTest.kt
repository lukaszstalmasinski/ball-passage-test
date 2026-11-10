package pl.stalmasinski.ballpassage.core.balls

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BallRangeTest {

    @Test
    fun `range of diameters`() {
        val range = BallRange.of(BigDecimal("20"), BigDecimal("22"), BigDecimal("0.5"))

        assertInstanceOf(BallRange.Result.Valid::class.java, range)
        assertEquals(listOf("20", "20.5", "21", "21.5", "22"), (range as BallRange.Result.Valid).diameters.map { it.toPlainString() })
    }

    @Test
    fun `range stops before passing the end`() {
        val range = BallRange.of(BigDecimal("20"), BigDecimal("21"), BigDecimal("0.3")) as BallRange.Result.Valid

        assertEquals(listOf("20", "20.3", "20.6", "20.9"), range.diameters.map { it.toPlainString() })
    }

    @Test
    fun `invalid ranges`() {
        fun error(from: String, to: String, step: String) =
            (BallRange.of(BigDecimal(from), BigDecimal(to), BigDecimal(step)) as BallRange.Result.Invalid).error

        assertEquals(BallRange.Error.FROM_NOT_POSITIVE, error("0", "10", "1"))
        assertEquals(BallRange.Error.STEP_NOT_POSITIVE, error("10", "20", "0"))
        assertEquals(BallRange.Error.TO_BELOW_FROM, error("20", "10", "1"))
        assertEquals(BallRange.Error.TOO_MANY, error("1", "1000", "1"))
        assertInstanceOf(BallRange.Result.Valid::class.java, BallRange.of(BigDecimal("1"), BigDecimal("200"), BigDecimal("1")))
    }
}
