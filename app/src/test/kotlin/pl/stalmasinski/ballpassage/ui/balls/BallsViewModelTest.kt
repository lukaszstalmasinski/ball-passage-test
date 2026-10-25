package pl.stalmasinski.ballpassage.ui.balls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.stalmasinski.ballpassage.MainDispatcherRule
import pl.stalmasinski.ballpassage.data.FakeUserDataRepository
import java.math.BigDecimal

class BallsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeUserDataRepository()
    private var nextId = 0
    private val viewModel by lazy { BallsViewModel(repository) { "id${nextId++}" } }

    private val state get() = viewModel.uiState.value
    private val diameters get() = state.balls.map { it.diameter.toPlainString() }

    @Test
    fun `adds a ball and clears the input`() {
        viewModel.onInputChange("31,8")

        assertTrue(viewModel.add())

        assertEquals(listOf("31.8"), diameters)
        assertEquals("", state.input)
        assertNull(state.inputError)
    }

    @Test
    fun `rejects invalid, zero and duplicate balls`() {
        viewModel.onInputChange("")
        assertFalse(viewModel.add())
        assertEquals(BallInputError.INVALID_NUMBER, state.inputError)

        viewModel.onInputChange("0")
        assertFalse(viewModel.add())
        assertEquals(BallInputError.MUST_BE_POSITIVE, state.inputError)

        viewModel.onInputChange("32")
        viewModel.add()
        viewModel.onInputChange("32.0")
        assertFalse(viewModel.add())
        assertEquals(BallInputError.DUPLICATE, state.inputError)
        assertEquals("32.0", state.input)
    }

    @Test
    fun `typing clears the error`() {
        viewModel.add()

        viewModel.onInputChange("3")

        assertNull(state.inputError)
    }

    @Test
    fun `remove and restore keeps the details`() {
        viewModel.onInputChange("32")
        viewModel.add()
        viewModel.updateDetails(state.balls[0], "K-12", "case 2")
        val ball = state.balls[0]

        viewModel.remove(ball)
        assertEquals(emptyList<String>(), diameters)

        viewModel.restore(ball)
        assertEquals(listOf(ball), state.balls)
        assertEquals("K-12 · case 2", state.balls[0].details())
    }

    @Test
    fun `adds a range and reports only new balls`() {
        viewModel.onInputChange("21")
        viewModel.add()

        val result = viewModel.addRange("20", "22,5", "0,5")

        assertEquals(RangeResult.Added(5), result)
        assertEquals(listOf("20", "20.5", "21", "21.5", "22", "22.5"), diameters)
    }

    @Test
    fun `invalid ranges`() {
        assertEquals(RangeResult.Invalid(RangeError.INVALID_NUMBER), viewModel.addRange("", "30", "1"))
        assertEquals(RangeResult.Invalid(RangeError.FROM_NOT_POSITIVE), viewModel.addRange("0", "30", "1"))
        assertEquals(RangeResult.Invalid(RangeError.STEP_NOT_POSITIVE), viewModel.addRange("20", "30", "0"))
        assertEquals(RangeResult.Invalid(RangeError.TO_BELOW_FROM), viewModel.addRange("30", "20", "1"))
        assertEquals(RangeResult.Invalid(RangeError.TOO_MANY), viewModel.addRange("1", "500", "1"))
        assertEquals(emptyList<String>(), diameters)
    }

    @Test
    fun `sets can be created, renamed, selected and deleted`() {
        viewModel.onInputChange("32")
        viewModel.add()
        val defaultSet = state.activeSet

        viewModel.createSet("Hall A")
        assertEquals("Hall A", state.activeSet.name)
        assertEquals(emptyList<String>(), diameters)
        assertTrue(state.canDeleteSet)

        viewModel.renameSet("Case 2")
        assertEquals(listOf("", "Case 2"), state.sets.map { it.name })

        viewModel.selectSet(defaultSet)
        assertEquals(listOf("32"), diameters)

        viewModel.deleteSet()
        assertEquals(listOf("Case 2"), state.sets.map { it.name })
        assertFalse(state.canDeleteSet)
    }

    @Test
    fun `exported set imports as a new active set`() {
        viewModel.onInputChange("32")
        viewModel.add()
        viewModel.updateDetails(state.balls[0], "K-12", "case 2")
        viewModel.onInputChange("35")
        viewModel.add()

        val text = viewModel.exportText("Hall A")
        val shared = viewModel.parseImport("Shared with you:\n\n$text\n\nBye")
        assertNotNull(shared)

        viewModel.importSet(shared!!)

        assertEquals(2, state.sets.size)
        assertEquals("Hall A", state.activeSet.name)
        assertEquals(listOf("32", "35"), diameters)
        assertEquals("K-12 · case 2", state.balls[0].details())
    }

    @Test
    fun `text without a set is not imported`() {
        assertNull(viewModel.parseImport("hello"))
    }

    @Test
    fun `range step default`() {
        assertEquals(BigDecimal.ONE, BallsViewModel.DEFAULT_RANGE_STEP)
    }
}
