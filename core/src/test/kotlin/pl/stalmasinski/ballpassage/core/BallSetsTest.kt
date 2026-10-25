package pl.stalmasinski.ballpassage.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class BallSetsTest {

    private var nextId = 0
    private val newId = { "id${nextId++}" }

    private fun initial() = BallSets.initial(newId())

    private fun BallSets.diameters(setId: String = activeSetId) = set(setId)!!.balls.map { it.diameter.toPlainString() }

    @Test
    fun `adds balls sorted, normalized and without duplicates`() {
        val sets = initial()
            .let { it.addBalls(it.activeSetId, listOf("35", "31.80", "35.0").map(::BigDecimal), newId) }
            .let { it.addBalls(it.activeSetId, listOf(BigDecimal("31.8"), BigDecimal("20")), newId) }

        assertEquals(listOf("20", "31.8", "35"), sets.diameters())
    }

    @Test
    fun `removes and restores a ball with its details`() {
        var sets = initial().let { it.addBalls(it.activeSetId, listOf(BigDecimal("32")), newId) }
        sets = sets.updateBallDetails(sets.activeSetId, sets.active.balls[0].id, " K-12 ", "case\t2")
        val ball = sets.active.balls[0]

        sets = sets.removeBall(sets.activeSetId, ball.id)
        assertEquals(emptyList<String>(), sets.diameters())

        sets = sets.restoreBall(sets.activeSetId, ball)
        assertEquals(listOf(Ball(ball.id, BigDecimal("32"), "K-12", "case 2")), sets.active.balls)
    }

    @Test
    fun `details are single line and limited in length`() {
        var sets = initial().let { it.addBalls(it.activeSetId, listOf(BigDecimal("32")), newId) }
        sets = sets.updateBallDetails(sets.activeSetId, sets.active.balls[0].id, "a\nb", "x".repeat(100))

        assertEquals("a b", sets.active.balls[0].label)
        assertEquals(BallSets.MAX_TEXT_LENGTH, sets.active.balls[0].location.length)
    }

    @Test
    fun `new set becomes active and names stay unique`() {
        var sets = initial().createSet("Hall A", newId())
        assertEquals("Hall A", sets.active.name)

        sets = sets.createSet("hall a", newId())
        assertEquals("hall a (2)", sets.active.name)
        assertEquals(3, sets.sets.size)
    }

    @Test
    fun `rename keeps the own name allowed`() {
        val sets = initial().createSet("Hall A", newId())

        assertEquals("Hall A", sets.renameSet(sets.activeSetId, "Hall A").active.name)
        assertEquals("Case", sets.renameSet(sets.activeSetId, "  Case ").active.name)
    }

    @Test
    fun `deleting the active set activates another one, the last set stays`() {
        val first = initial()
        val sets = first.createSet("Hall A", newId())

        val afterDelete = sets.deleteSet(sets.activeSetId)
        assertEquals(listOf(first.activeSetId), afterDelete.sets.map { it.id })
        assertEquals(first.activeSetId, afterDelete.activeSetId)

        assertSame(afterDelete, afterDelete.deleteSet(afterDelete.activeSetId))
    }

    @Test
    fun `select ignores unknown sets`() {
        val sets = initial().createSet("Hall A", newId())
        val firstId = sets.sets.first().id

        assertEquals(firstId, sets.selectSet(firstId).activeSetId)
        assertEquals(sets.activeSetId, sets.selectSet("unknown").activeSetId)
    }

    @Test
    fun `import adds an active set with new ids and a unique name`() {
        val sets = initial().createSet("Hall A", newId())
        val shared = SharedBallSet(
            name = "Hall A",
            balls = listOf(SharedBall(BigDecimal("35")), SharedBall(BigDecimal("32.0"), "K-12"), SharedBall(BigDecimal("32"))),
        )

        val imported = sets.importSet(shared, newId)

        assertEquals("Hall A (2)", imported.active.name)
        assertEquals(listOf("32", "35"), imported.diameters())
        assertEquals("K-12", imported.active.balls[0].label)
        assertEquals(3, imported.sets.size)
    }

    @Test
    fun `there is always a set`() {
        assertThrows<IllegalArgumentException> { BallSets(emptyList(), "x") }
    }

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
