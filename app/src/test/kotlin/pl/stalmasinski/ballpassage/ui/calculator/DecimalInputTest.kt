package pl.stalmasinski.ballpassage.ui.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class DecimalInputTest {

    @Test
    fun `parses dot and comma separators`() {
        assertEquals(BigDecimal("48.3"), DecimalInput.parse("48.3"))
        assertEquals(BigDecimal("48.3"), DecimalInput.parse("48,3"))
        assertEquals(BigDecimal("12"), DecimalInput.parse("12"))
        assertEquals(BigDecimal("12"), DecimalInput.parse("12."))
        assertEquals(BigDecimal("0.5"), DecimalInput.parse(",5"))
    }

    @Test
    fun `rejects malformed numbers`() {
        listOf("", ".", ",", "1.2.3", "1,2,3", "1e5", "-1", "abc", "1 2").forEach {
            assertNull("\"$it\" should be rejected", DecimalInput.parse(it))
        }
    }

    @Test
    fun `sanitize keeps only digits and separators`() {
        assertEquals("48.3", DecimalInput.sanitize(" 48.3 mm"))
        assertEquals("12,5", DecimalInput.sanitize("-12,5"))
        // Non-ASCII digits are dropped so that parsing stays unambiguous.
        assertEquals("", DecimalInput.sanitize("٤٨"))
    }

    @Test
    fun `sanitize limits the length`() {
        assertEquals(DecimalInput.MAX_LENGTH, DecimalInput.sanitize("1234567890123").length)
    }
}
