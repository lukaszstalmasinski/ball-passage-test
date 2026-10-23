package pl.stalmasinski.ballpassage.ui.balls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private val viewModel by lazy { BallsViewModel(repository) }

    @Test
    fun `adds a ball and clears the input`() {
        viewModel.onInputChange("31,8")

        assertTrue(viewModel.add())

        assertEquals(listOf(BigDecimal("31.8")), viewModel.uiState.value.balls)
        assertEquals("", viewModel.uiState.value.input)
        assertNull(viewModel.uiState.value.inputError)
    }

    @Test
    fun `rejects invalid, zero and duplicate balls`() {
        viewModel.onInputChange("")
        assertFalse(viewModel.add())
        assertEquals(BallInputError.INVALID_NUMBER, viewModel.uiState.value.inputError)

        viewModel.onInputChange("0")
        assertFalse(viewModel.add())
        assertEquals(BallInputError.MUST_BE_POSITIVE, viewModel.uiState.value.inputError)

        viewModel.onInputChange("32")
        viewModel.add()
        viewModel.onInputChange("32.0")
        assertFalse(viewModel.add())
        assertEquals(BallInputError.DUPLICATE, viewModel.uiState.value.inputError)
        assertEquals("32.0", viewModel.uiState.value.input)
    }

    @Test
    fun `typing clears the error`() {
        viewModel.add()

        viewModel.onInputChange("3")

        assertNull(viewModel.uiState.value.inputError)
    }

    @Test
    fun `remove and restore`() {
        viewModel.onInputChange("32")
        viewModel.add()

        viewModel.remove(BigDecimal("32"))
        assertEquals(emptyList<BigDecimal>(), viewModel.uiState.value.balls)

        viewModel.restore(BigDecimal("32"))
        assertEquals(listOf(BigDecimal("32")), viewModel.uiState.value.balls)
    }
}
