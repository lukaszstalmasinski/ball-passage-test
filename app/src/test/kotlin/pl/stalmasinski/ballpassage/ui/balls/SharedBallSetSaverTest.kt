package pl.stalmasinski.ballpassage.ui.balls

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.stalmasinski.ballpassage.core.balls.SharedBall
import pl.stalmasinski.ballpassage.core.balls.SharedBallSet
import java.math.BigDecimal

class SharedBallSetSaverTest {

    private val scope = SaverScope { true }

    private fun roundTrip(set: SharedBallSet?): SharedBallSet? {
        val saved = with(SharedBallSetSaver) { scope.save(set) }
        return saved?.let { SharedBallSetSaver.restore(it) }
    }

    @Test
    fun `a set with and without ball details survives saving`() {
        val set = SharedBallSet(
            name = "Hall B",
            balls = listOf(
                SharedBall(BigDecimal("31.8"), "K-12", "case 2"),
                SharedBall(BigDecimal("32")),
                SharedBall(BigDecimal("35"), location = "drawer"),
            ),
        )

        assertEquals(set, roundTrip(set))
    }

    @Test
    fun `an unnamed set survives saving`() {
        val set = SharedBallSet(name = "", balls = listOf(SharedBall(BigDecimal("40"))))

        assertEquals(set, roundTrip(set))
    }

    @Test
    fun `no pending set stays empty`() {
        assertNull(roundTrip(null))
    }
}
