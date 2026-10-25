@file:OptIn(ExperimentalCoroutinesApi::class)

package pl.stalmasinski.ballpassage.ui.calculator

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.stalmasinski.ballpassage.MainDispatcherRule
import pl.stalmasinski.ballpassage.core.CalculationProblem
import pl.stalmasinski.ballpassage.core.CalculationResult
import pl.stalmasinski.ballpassage.core.TubeDimensions
import pl.stalmasinski.ballpassage.data.FakeUserDataRepository
import java.math.BigDecimal

class CalculatorViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val repository = FakeUserDataRepository()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = CalculatorViewModel(handle, repository)

    private val CalculatorViewModel.state get() = uiState.value

    @Test
    fun `initial state is empty`() = runTest(mainDispatcher.dispatcher) {
        val state = viewModel().state

        assertNull(state.result)
        assertFalse(state.hasFieldErrors)
        assertFalse(state.hasInput)
        DimensionField.entries.forEach { assertEquals(FieldState(), state.field(it)) }
    }

    @Test
    fun `calculates once all fields are filled`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()

        viewModel.onValueChange(DimensionField.OUTER_DIAMETER, "48,3")
        viewModel.onValueChange(DimensionField.WALL_THICKNESS, "5")
        runCurrent()
        assertNull(viewModel.state.result)

        viewModel.onValueChange(DimensionField.BEND_RADIUS, "70")
        runCurrent()
        val result = viewModel.state.result as CalculationResult.Success
        assertEquals(0, BigDecimal("31.8").compareTo(result.minBallDiameter))
    }

    @Test
    fun `suggests fitting balls from the set`() = runTest(mainDispatcher.dispatcher) {
        var id = 0
        repository.updateBallSets { sets ->
            sets.addBalls(sets.activeSetId, listOf("30", "35", "32", "40").map(::BigDecimal)) { "b${id++}" }
                .renameSet(sets.activeSetId, "Hall A")
        }
        val viewModel = viewModel().filled()
        runCurrent()

        assertTrue(viewModel.state.hasBallSet)
        assertEquals("Hall A", viewModel.state.ballSetName)
        assertEquals(listOf("32", "35"), viewModel.state.fittingBalls.map { it.diameter.toPlainString() })
    }

    @Test
    fun `clearing a field removes the result`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel().filled()

        viewModel.onValueChange(DimensionField.BEND_RADIUS, "")
        runCurrent()

        assertNull(viewModel.state.result)
        assertFalse(viewModel.state.hasFieldErrors)
    }

    @Test
    fun `clear all empties every field`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel().filled()

        viewModel.clearAll()
        runCurrent()

        assertFalse(viewModel.state.hasInput)
        assertNull(viewModel.state.result)
    }

    @Test
    fun `invalid number is reported on the field`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel().filled()

        viewModel.onValueChange(DimensionField.OUTER_DIAMETER, "48.3.1")
        runCurrent()

        assertEquals(FieldError.INVALID_NUMBER, viewModel.state.field(DimensionField.OUTER_DIAMETER).error)
        assertNull(viewModel.state.result)
    }

    @Test
    fun `zero is rejected for diameter and wall but allowed for bend radius`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel().filled()

        viewModel.onValueChange(DimensionField.BEND_RADIUS, "0")
        runCurrent()
        assertTrue(viewModel.state.result is CalculationResult.Success)

        viewModel.onValueChange(DimensionField.WALL_THICKNESS, "0")
        runCurrent()
        assertEquals(FieldError.MUST_BE_POSITIVE, viewModel.state.field(DimensionField.WALL_THICKNESS).error)

        viewModel.onValueChange(DimensionField.WALL_THICKNESS, "5")
        viewModel.onValueChange(DimensionField.OUTER_DIAMETER, "0")
        runCurrent()
        assertEquals(FieldError.MUST_BE_POSITIVE, viewModel.state.field(DimensionField.OUTER_DIAMETER).error)
    }

    @Test
    fun `wall too thick is reported on the wall field`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel().filled()

        viewModel.onValueChange(DimensionField.WALL_THICKNESS, "30")
        runCurrent()

        assertEquals(FieldError.WALL_TOO_THICK, viewModel.state.field(DimensionField.WALL_THICKNESS).error)
        assertNull(viewModel.state.result)
    }

    @Test
    fun `range problems are shown as a failed result`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel().filled()

        viewModel.onValueChange(DimensionField.BEND_RADIUS, "10")
        runCurrent()

        val result = viewModel.state.result as CalculationResult.Failure
        assertEquals(setOf(CalculationProblem.BEND_RATIO_TOO_SMALL), result.problems)
        assertFalse(viewModel.state.hasFieldErrors)
    }

    @Test
    fun `input is sanitized`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()

        viewModel.onValueChange(DimensionField.OUTER_DIAMETER, "-48.3 mm")
        runCurrent()

        assertEquals("48.3", viewModel.state.field(DimensionField.OUTER_DIAMETER).text)
    }

    @Test
    fun `inputs survive process death`() = runTest(mainDispatcher.dispatcher) {
        val handle = SavedStateHandle()
        viewModel(handle).filled()

        val restored = viewModel(handle)
        runCurrent()

        assertEquals("48.3", restored.state.field(DimensionField.OUTER_DIAMETER).text)
        assertTrue(restored.state.result is CalculationResult.Success)
    }

    @Test
    fun `result is added to history only after the input settles`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        viewModel.onValueChange(DimensionField.OUTER_DIAMETER, "48.3")
        viewModel.onValueChange(DimensionField.WALL_THICKNESS, "5")
        viewModel.onValueChange(DimensionField.BEND_RADIUS, "7")
        advanceTimeBy(500)
        viewModel.onValueChange(DimensionField.BEND_RADIUS, "70")

        advanceTimeBy(CalculatorViewModel.HISTORY_DELAY_MILLIS - 1)
        assertEquals(emptyList<TubeDimensions>(), repository.history.value)

        advanceTimeBy(2)
        assertEquals(listOf(tube("48.3", "5", "70")), repository.history.value)
        assertEquals(repository.history.value, viewModel.state.history)
    }

    @Test
    fun `failed results are not added to history`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        viewModel.onValueChange(DimensionField.OUTER_DIAMETER, "48.3")
        viewModel.onValueChange(DimensionField.WALL_THICKNESS, "5")
        viewModel.onValueChange(DimensionField.BEND_RADIUS, "10")

        advanceTimeBy(CalculatorViewModel.HISTORY_DELAY_MILLIS * 2)

        assertEquals(emptyList<TubeDimensions>(), repository.history.value)
    }

    @Test
    fun `applying a history entry fills the fields with the locale separator`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()

        viewModel.applyHistory(tube("48.3", "5", "70"), decimalSeparator = ',')
        runCurrent()

        assertEquals("48,3", viewModel.state.field(DimensionField.OUTER_DIAMETER).text)
        assertTrue(viewModel.state.result is CalculationResult.Success)
    }

    @Test
    fun `clear history`() = runTest(mainDispatcher.dispatcher) {
        repository.addToHistory(tube("48.3", "5", "70"))
        val viewModel = viewModel()

        viewModel.clearHistory()
        runCurrent()

        assertEquals(emptyList<TubeDimensions>(), viewModel.state.history)
    }

    private fun CalculatorViewModel.filled() = apply {
        onValueChange(DimensionField.OUTER_DIAMETER, "48.3")
        onValueChange(DimensionField.WALL_THICKNESS, "5")
        onValueChange(DimensionField.BEND_RADIUS, "70")
    }

    private fun tube(outer: String, wall: String, radius: String) =
        TubeDimensions(BigDecimal(outer), BigDecimal(wall), BigDecimal(radius))
}
