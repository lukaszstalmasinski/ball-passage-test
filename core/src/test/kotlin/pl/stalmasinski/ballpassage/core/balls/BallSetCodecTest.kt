package pl.stalmasinski.ballpassage.core.balls

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BallSetCodecTest {

    @Test
    fun `codec round trip`() {
        val set = BallSet(
            id = "s",
            name = "Hall A",
            balls = listOf(
                Ball("1", BigDecimal("31.8"), "K-12", "case 2"),
                Ball("2", BigDecimal("32")),
                Ball("3", BigDecimal("35"), location = "drawer"),
            ),
        )

        val text = BallSetCodec.encode(set)

        assertEquals("BPT1\nHall A\n31.8\tK-12\tcase 2\n32\n35\t\tdrawer", text)
        assertEquals(
            SharedBallSet(
                "Hall A",
                listOf(
                    SharedBall(BigDecimal("31.8"), "K-12", "case 2"),
                    SharedBall(BigDecimal("32")),
                    SharedBall(BigDecimal("35"), location = "drawer"),
                ),
            ),
            BallSetCodec.decode(text),
        )
    }

    @Test
    fun `decode finds the block inside a pasted message`() {
        val message = "Ball set from Ball Passage Test:\r\n\r\nBPT1\r\nCase\r\n32,5\r\nabc\r\n-1\r\n40\r\n\r\nSent from my phone"

        val decoded = BallSetCodec.decode(message)!!

        assertEquals("Case", decoded.name)
        assertEquals(listOf("32.5", "40"), decoded.balls.map { it.diameter.toPlainString() })
    }

    @Test
    fun `decode rejects text without a set`() {
        assertNull(BallSetCodec.decode("hello"))
        assertNull(BallSetCodec.decode("BPT1"))
        assertNull(BallSetCodec.decode("BPT1\nName\nabc"))
    }
}
