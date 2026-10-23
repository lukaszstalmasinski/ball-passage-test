package pl.stalmasinski.ballpassage.ui.calculator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.stalmasinski.ballpassage.BallPassageApplication
import pl.stalmasinski.ballpassage.core.BallPassageCalculator
import pl.stalmasinski.ballpassage.core.BallSelector
import pl.stalmasinski.ballpassage.core.CalculationProblem
import pl.stalmasinski.ballpassage.core.CalculationResult
import pl.stalmasinski.ballpassage.core.TubeDimensions
import pl.stalmasinski.ballpassage.data.UserDataRepository
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
    /** Balls from the user's set that fit [result], recommended one first. */
    val fittingBalls: List<BigDecimal> = emptyList(),
    val hasBallSet: Boolean = false,
    val history: List<TubeDimensions> = emptyList(),
) {
    fun field(field: DimensionField): FieldState = fields.getValue(field)

    val hasFieldErrors: Boolean get() = fields.values.any { it.error != null }

    val hasInput: Boolean get() = fields.values.any { it.text.isNotEmpty() }
}

class CalculatorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: UserDataRepository,
    private val historyDelayMillis: Long = HISTORY_DELAY_MILLIS,
) : ViewModel() {

    private val inputs = MutableStateFlow(
        DimensionField.entries.associateWith { savedStateHandle.get<String>(it.name).orEmpty() },
    )

    val uiState: StateFlow<CalculatorUiState> =
        combine(inputs, repository.balls, repository.history, ::buildState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, buildState(inputs.value, emptyList(), emptyList()))

    private var historyJob: Job? = null

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
        viewModelScope.launch { repository.clearHistory() }
    }

    private fun setInputs(values: Map<DimensionField, String>) {
        values.forEach { (field, text) -> savedStateHandle[field.name] = text }
        inputs.value = values
        scheduleHistoryEntry()
    }

    /**
     * Results are added to the history only once the input has stayed unchanged for a moment,
     * so that intermediate values typed on the way (4 → 48 → 48.3) are not recorded.
     */
    private fun scheduleHistoryEntry() {
        historyJob?.cancel()
        val result = calculate(inputs.value).result as? CalculationResult.Success ?: return
        historyJob = viewModelScope.launch {
            delay(historyDelayMillis)
            repository.addToHistory(result.dimensions)
        }
    }

    companion object {
        const val HISTORY_DELAY_MILLIS = 2_000L

        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as BallPassageApplication
                CalculatorViewModel(createSavedStateHandle(), application.userDataRepository)
            }
        }

        internal fun buildState(
            inputs: Map<DimensionField, String>,
            balls: List<BigDecimal>,
            history: List<TubeDimensions>,
        ): CalculatorUiState {
            val calculated = calculate(inputs)
            val result = calculated.result
            return calculated.copy(
                fittingBalls = if (result is CalculationResult.Success) BallSelector.fittingBalls(result, balls) else emptyList(),
                hasBallSet = balls.isNotEmpty(),
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
