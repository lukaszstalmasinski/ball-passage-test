package pl.stalmasinski.ballpassage.ui.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import pl.stalmasinski.ballpassage.core.Ball
import pl.stalmasinski.ballpassage.core.BallSet
import pl.stalmasinski.ballpassage.core.BallSetCodec
import java.math.BigDecimal

class QrCodesTest {

    @Test
    fun `generated code decodes back to the shared set`() {
        val set = BallSet(
            id = "s",
            name = "Hala A – walizka",
            balls = (20..45).map { Ball("$it", BigDecimal(it), label = "K-$it", location = "przegródka ${it % 6}") },
        )
        val text = BallSetCodec.encode(set)

        val decoded = decodeRendered(text, scale = 4, rowPadding = 16)

        assertEquals(text, decoded)
        assertEquals(26, BallSetCodec.decode(decoded!!)!!.balls.size)
    }

    @Test
    fun `too large sets do not get a code`() {
        val text = "x".repeat(QrCodes.MAX_BYTES + 1)

        assertFalse(QrCodes.fits(text))
        assertNull(QrCodes.encode(text))
    }

    @Test
    fun `image without a code decodes to null`() {
        assertNull(QrCodes.decode(ByteArray(100 * 100) { 127 }, 100, 100))
    }

    /**
     * Renders the code like a camera frame: grayscale bytes, scaled up, with extra bytes at the end
     * of every row as in camera buffers whose row stride is larger than the width.
     */
    private fun decodeRendered(text: String, scale: Int, rowPadding: Int): String? {
        val matrix = QrCodes.encode(text)
        assertNotNull(matrix)
        matrix!!
        val width = matrix.width * scale
        val height = matrix.height * scale
        val stride = width + rowPadding
        val pixels = ByteArray(stride * height) { index ->
            val x = index % stride
            val y = index / stride
            if (x < width && matrix[x / scale, y / scale]) 0 else 255.toByte()
        }
        return QrCodes.decode(pixels, width, height, stride)
    }
}
