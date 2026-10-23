package pl.stalmasinski.ballpassage.ui.calculator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.stalmasinski.ballpassage.core.BallPassageCalculator
import pl.stalmasinski.ballpassage.core.CalculationProblem
import pl.stalmasinski.ballpassage.core.CalculationResult
import pl.stalmasinski.ballpassage.core.TubeDimensions
import java.math.BigDecimal

/** Input fields in the order they are shown. */
enum class DimensionField {
    OUTER_DIAMETER,
    WALL_THICKNESS,
    BEND_RADIUS,
}

enum class FieldError {
    INVALID_NUMBER,
    MUST_BE_POSITIVE,
    WALL_TOO_THICK,
}

data class FieldState(val text: String = "", val error: FieldError? = null)

data class CalculatorUiState(
    val fields: Map<DimensionField, FieldState> = DimensionField.entries.associateWith { FieldState() },
    /** Null until all fields hold valid values. */
    val result: CalculationResult? = null,
) {
    fun field(field: DimensionField): FieldState = fields.getValue(field)

    val hasFieldErrors: Boolean get() = fields.values.any { it.error != null }
}

class CalculatorViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _uiState = MutableStateFlow(buildState(readInputs()))
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    fun onValueChange(field: DimensionField, text: String) {
        savedStateHandle[field.name] = DecimalInput.sanitize(text)
        _uiState.value = buildState(readInputs())
    }

    private fun readInputs(): Map<DimensionField, String> =
        DimensionField.entries.associateWith { savedStateHandle.get<String>(it.name).orEmpty() }

    companion object {
        val Factory = viewModelFactory {
            initializer { CalculatorViewModel(createSavedStateHandle()) }
        }

        internal fun buildState(inputs: Map<DimensionField, String>): CalculatorUiState {
            val values = mutableMapOf<DimensionField, BigDecimal>()
            val errors = mutableMapOf<DimensionField, FieldError>()

            for ((field, text) in inputs) {
                if (text.isBlank()) continue
                val value = DecimalInput.parse(text)
                when {
                    value == null -> errors[field] = FieldError.INVALID_NUMBER
                    field != DimensionField.BEND_RADIUS && value.signum() == 0 ->
                        errors[field] = FieldError.MUST_BE_POSITIVE
                    else -> values[field] = value
                }
            }

            var result: CalculationResult? = null
            if (errors.isEmpty() && values.size == DimensionField.entries.size) {
                val calculated = BallPassageCalculator.calculate(
                    TubeDimensions(
                        outerDiameter = values.getValue(DimensionField.OUTER_DIAMETER),
                        wallThickness = values.getValue(DimensionField.WALL_THICKNESS),
                        bendRadius = values.getValue(DimensionField.BEND_RADIUS),
                    ),
                )
                if (calculated is CalculationResult.Failure &&
                    CalculationProblem.WALL_TOO_THICK in calculated.problems
                ) {
                    errors[DimensionField.WALL_THICKNESS] = FieldError.WALL_TOO_THICK
                } else {
                    result = calculated
                }
            }

            return CalculatorUiState(
                fields = inputs.mapValues { (field, text) -> FieldState(text, errors[field]) },
                result = result,
            )
        }
    }
}
