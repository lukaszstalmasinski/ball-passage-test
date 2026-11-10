package pl.stalmasinski.ballpassage.ui.calculator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
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
import pl.stalmasinski.ballpassage.core.balls.Ball
import pl.stalmasinski.ballpassage.core.balls.BallSelector
import pl.stalmasinski.ballpassage.core.balls.BallSet
import pl.stalmasinski.ballpassage.core.calculation.BallPassageCalculator
import pl.stalmasinski.ballpassage.core.calculation.CalculationProblem
import pl.stalmasinski.ballpassage.core.calculation.CalculationResult
import pl.stalmasinski.ballpassage.core.calculation.TubeDimensions
import pl.stalmasinski.ballpassage.data.UserDataRepository
import pl.stalmasinski.ballpassage.ui.common.DecimalInput
import java.io.IOException
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
    /** Balls from the active set that fit [result], recommended one first. */
    val fittingBalls: List<Ball> = emptyList(),
    val hasBallSet: Boolean = false,
    /** Name of the active ball set; empty for the default set. */
    val ballSetName: String = "",
    val history: List<TubeDimensions> = emptyList(),
) {
    fun field(field: DimensionField): FieldState = fields.getValue(field)

    val hasFieldErrors: Boolean get() = fields.values.any { it.error != null }

    val hasInput: Boolean get() = fields.values.any { it.text.isNotEmpty() }
}

class CalculatorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: UserDataRepository,
) : ViewModel() {

    private val inputs = MutableStateFlow(
        DimensionField.entries.associateWith { savedStateHandle.get<String>(it.name).orEmpty() },
    )

    val uiState: StateFlow<CalculatorUiState> =
        combine(inputs, repository.ballSets, repository.history) { inputs, ballSets, history ->
            buildState(inputs, ballSets.active, history)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, buildState(inputs.value, EMPTY_SET, emptyList()))

    fun onValueChange(field: DimensionField, text: String) {
        setInputs(inputs.value + (field to DecimalInput.sanitize(text)))
    }

    fun clearAll() {
        setInputs(DimensionField.entries.associateWith { "" })
    }

    /** Fills all fields from a history entry, using [decimalSeparator] of the current locale. */
    fun applyHistory(dimensions: TubeDimensions, decimalSeparator: Char) {
        setInputs(
            mapOf(
                DimensionField.OUTER_DIAMETER to DecimalInput.format(dimensions.outerDiameter, decimalSeparator),
                DimensionField.WALL_THICKNESS to DecimalInput.format(dimensions.wallThickness, decimalSeparator),
                DimensionField.BEND_RADIUS to DecimalInput.format(dimensions.bendRadius, decimalSeparator),
            ),
        )
    }

    fun clearHistory() {
        viewModelScope.launch { saveHistory { repository.clearHistory() } }
    }

    private fun setInputs(values: Map<DimensionField, String>) {
        values.forEach { (field, text) -> savedStateHandle[field.name] = text }
        inputs.value = values
    }

    /**
     * Adds the calculated tube to the recent tubes. The screen calls it when the user is done with
     * the input (the keyboard closes or the app goes to the background), so that tubes typed on the
     * way, one field at a time, are not recorded.
     */
    fun saveToHistory() {
        val result = calculate(inputs.value).result as? CalculationResult.Success ?: return
        viewModelScope.launch { saveHistory { repository.addToHistory(result.dimensions) } }
    }

    /** Recent tubes are a convenience: when they cannot be saved, the calculator carries on without them. */
    private suspend fun saveHistory(write: suspend () -> Unit) {
        try {
            write()
        } catch (e: IOException) {
            // Nothing to show: the result on screen is unaffected.
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as BallPassageApplication
                CalculatorViewModel(createSavedStateHandle(), application.userDataRepository)
            }
        }

        private val EMPTY_SET = BallSet(id = "", name = "")

        internal fun buildState(
            inputs: Map<DimensionField, String>,
            ballSet: BallSet,
            history: List<TubeDimensions>,
        ): CalculatorUiState {
            val calculated = calculate(inputs)
            val result = calculated.result
            return calculated.copy(
                fittingBalls = if (result is CalculationResult.Success) {
                    BallSelector.fittingBalls(result, ballSet.balls)
                } else {
                    emptyList()
                },
                hasBallSet = ballSet.balls.isNotEmpty(),
                ballSetName = ballSet.name,
                history = history,
            )
        }

        private fun calculate(inputs: Map<DimensionField, String>): CalculatorUiState {
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
                fields = DimensionField.entries.associateWith { FieldState(inputs[it].orEmpty(), errors[it]) },
                result = result,
            )
        }
    }
}
