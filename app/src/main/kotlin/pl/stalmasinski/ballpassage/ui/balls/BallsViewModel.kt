package pl.stalmasinski.ballpassage.ui.balls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.stalmasinski.ballpassage.BallPassageApplication
import pl.stalmasinski.ballpassage.data.UserDataRepository
import pl.stalmasinski.ballpassage.ui.calculator.DecimalInput
import java.math.BigDecimal

enum class BallInputError {
    INVALID_NUMBER,
    MUST_BE_POSITIVE,
    DUPLICATE,
}

data class BallsUiState(
    val balls: List<BigDecimal> = emptyList(),
    val input: String = "",
    val inputError: BallInputError? = null,
)

class BallsViewModel(private val repository: UserDataRepository) : ViewModel() {

    private val input = MutableStateFlow("")
    private val inputError = MutableStateFlow<BallInputError?>(null)

    val uiState: StateFlow<BallsUiState> =
        combine(repository.balls, input, inputError, ::BallsUiState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, BallsUiState())

    fun onInputChange(text: String) {
        input.value = DecimalInput.sanitize(text)
        inputError.value = null
    }

    /** Adds the typed ball; returns false and sets an error when the input is not accepted. */
    fun add(): Boolean {
        val value = DecimalInput.parse(input.value)
        inputError.value = when {
            value == null -> BallInputError.INVALID_NUMBER
            value.signum() == 0 -> BallInputError.MUST_BE_POSITIVE
            uiState.value.balls.any { it.compareTo(value) == 0 } -> BallInputError.DUPLICATE
            else -> null
        }
        if (value == null || inputError.value != null) return false

        viewModelScope.launch { repository.addBall(value) }
        input.value = ""
        return true
    }

    fun remove(diameter: BigDecimal) {
        viewModelScope.launch { repository.removeBall(diameter) }
    }

    /** Puts back a ball removed by mistake. */
    fun restore(diameter: BigDecimal) {
        viewModelScope.launch { repository.addBall(diameter) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as BallPassageApplication
                BallsViewModel(application.userDataRepository)
            }
        }
    }
}
