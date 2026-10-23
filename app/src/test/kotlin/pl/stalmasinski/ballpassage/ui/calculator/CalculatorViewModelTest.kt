package pl.stalmasinski.ballpassage.ui.calculator

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.stalmasinski.ballpassage.core.CalculationProblem
import pl.stalmasinski.ballpassage.core.CalculationResult
import java.math.BigDecimal

class CalculatorViewModelTest {

    @Test
    fun `initial state is empty`() {
        val state = CalculatorViewModel(SavedStateHandle()).uiState.value

        assertNull(state.result)
        assertFalse(state.hasFieldErrors)
        DimensionField.entries.forEach { assertEquals(FieldState(), state.field(it)) }
    }

    @Test
    fun `calculates once all fields are filled`() {
        val viewModel = CalculatorViewModel(SavedStateHandle())

        viewModel.onValueChange(DimensionField.OUTER_DIAMETER, "48,3")
        viewModel.onValueChange(DimensionField.WALL_THICKNESS, "5")
        assertNull(viewModel.uiState.value.result)

        viewModel.onValueChange(DimensionField.BEND_RADIUS, "70")
        val result = viewModel.uiState.value.result as CalculationResult.Success
        assertEquals(0, BigDecimal("31.8").compareTo(result.ballDiameter))
    }

    @Test
    fun `clearing a field removes the result`() {
        val viewModel = filledViewModel()

        viewModel.onValueChange(DimensionField.BEND_RADIUS, "")

        assertNull(viewModel.uiState.value.result)
        assertFalse(viewModel.uiState.value.hasFieldErrors)
    }

    @Test
    fun `invalid number is reported on the field`() {
        val viewModel = filledViewModel()

        viewModel.onValueChange(DimensionField.OUTER_DIAMETER, "48.3.1")

        val state = viewModel.uiState.value
        assertEquals(FieldError.INVALID_NUMBER, state.field(DimensionField.OUTER_DIAMETER).error)
        assertNull(state.result)
    }

    @Test
    fun `zero is rejected for diameter and wall but allowed for bend radius`() {
        val viewModel = filledViewModel()

        viewModel.onValueChange(DimensionField.BEND_RADIUS, "0")
        assertTrue(viewModel.uiState.value.result is CalculationResult.Success)

        viewModel.onValueChange(DimensionField.WALL_THICKNESS, "0")
        assertEquals(FieldError.MUST_BE_POSITIVE, viewModel.uiState.value.field(DimensionField.WALL_THICKNESS).error)

        viewModel.onValueChange(DimensionField.WALL_THICKNESS, "5")
        viewModel.onValueChange(DimensionField.OUTER_DIAMETER, "0")
        assertEquals(FieldError.MUST_BE_POSITIVE, viewModel.uiState.value.field(DimensionField.OUTER_DIAMETER).error)
    }

    @Test
    fun `wall too thick is reported on the wall field`() {
        val viewModel = filledViewModel()

        viewModel.onValueChange(DimensionField.WALL_THICKNESS, "30")

        val state = viewModel.uiState.value
        assertEquals(FieldError.WALL_TOO_THICK, state.field(DimensionField.WALL_THICKNESS).error)
        assertNull(state.result)
    }

    @Test
    fun `range problems are shown as a failed result`() {
        val viewModel = filledViewModel()

        viewModel.onValueChange(DimensionField.BEND_RADIUS, "10")

        val result = viewModel.uiState.value.result as CalculationResult.Failure
        assertEquals(setOf(CalculationProblem.BEND_RATIO_TOO_SMALL), result.problems)
        assertFalse(viewModel.uiState.value.hasFieldErrors)
    }

    @Test
    fun `input is sanitized`() {
        val viewModel = CalculatorViewModel(SavedStateHandle())

        viewModel.onValueChange(DimensionField.OUTER_DIAMETER, "-48.3 mm")

        assertEquals("48.3", viewModel.uiState.value.field(DimensionField.OUTER_DIAMETER).text)
    }

    @Test
    fun `inputs survive process death`() {
        val handle = SavedStateHandle()
        CalculatorViewModel(handle).apply {
            onValueChange(DimensionField.OUTER_DIAMETER, "48.3")
            onValueChange(DimensionField.WALL_THICKNESS, "5")
            onValueChange(DimensionField.BEND_RADIUS, "70")
        }

        val restored = CalculatorViewModel(handle).uiState.value

        assertEquals("48.3", restored.field(DimensionField.OUTER_DIAMETER).text)
        assertTrue(restored.result is CalculationResult.Success)
    }

    private fun filledViewModel() = CalculatorViewModel(SavedStateHandle()).apply {
        onValueChange(DimensionField.OUTER_DIAMETER, "48.3")
        onValueChange(DimensionField.WALL_THICKNESS, "5")
        onValueChange(DimensionField.BEND_RADIUS, "70")
    }
}
