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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.stalmasinski.ballpassage.BallPassageApplication
import pl.stalmasinski.ballpassage.core.Ball
import pl.stalmasinski.ballpassage.core.BallPassageCalculator
import pl.stalmasinski.ballpassage.core.BallRequirement
import pl.stalmasinski.ballpassage.core.BallSelector
import pl.stalmasinski.ballpassage.core.BallSet
import pl.stalmasinski.ballpassage.core.CalculationProblem
import pl.stalmasinski.ballpassage.core.CalculationResult
import pl.stalmasinski.ballpassage.core.KtaCalculator
import pl.stalmasinski.ballpassage.core.KtaProblem
import pl.stalmasinski.ballpassage.core.KtaResult
import pl.stalmasinski.ballpassage.core.KtaTube
import pl.stalmasinski.ballpassage.core.Standard
import pl.stalmasinski.ballpassage.core.Tube
import pl.stalmasinski.ballpassage.core.TubeDimensions
import pl.stalmasinski.ballpassage.data.UserDataRepository
import java.math.BigDecimal

/** Input fields; each standard shows the ones in [Standard.fields], in that order. */
enum class DimensionField {
    OUTER_DIAMETER,
    WALL_THICKNESS,
    BEND_RADIUS,

    /** KTA 3211.1: largest permissible undersize. */
    UNDERSIZE,
    ;

    /** Zero is a meaningful value: a straight tube, or no undersize. */
    val allowsZero: Boolean get() = this == BEND_RADIUS || this == UNDERSIZE
}

val Standard.fields: List<DimensionField>
    get() = when (this) {
        Standard.VGB_S_013 -> listOf(DimensionField.OUTER_DIAMETER, DimensionField.WALL_THICKNESS, DimensionField.BEND_RADIUS)
        Standard.KTA_3211_1 -> listOf(DimensionField.OUTER_DIAMETER, DimensionField.WALL_THICKNESS, DimensionField.UNDERSIZE)
    }

enum class FieldError {
    INVALID_NUMBER,
    MUST_BE_POSITIVE,
    WALL_TOO_THICK,
}

data class FieldState(val text: String = "", val error: FieldError? = null)

data class CalculatorUiState(
    val standard: Standard = Standard.VGB_S_013,
    val fields: Map<DimensionField, FieldState> = DimensionField.entries.associateWith { FieldState() },
    /** VGB-S-013 result; null until all fields hold valid values, and for other standards. */
    val result: CalculationResult? = null,
    /** KTA 3211.1 result; null until all fields hold valid values, and for other standards. */
    val ktaResult: KtaResult? = null,
    /** Balls from the active set that fit the result, recommended one first. */
    val fittingBalls: List<Ball> = emptyList(),
    val hasBallSet: Boolean = false,
    /** Name of the active ball set; empty for the default set. */
    val ballSetName: String = "",
    val history: List<Tube> = emptyList(),
) {
    fun field(field: DimensionField): FieldState = fields.getValue(field)

    val visibleFields: List<DimensionField> get() = standard.fields

    val hasFieldErrors: Boolean get() = visibleFields.any { field(it).error != null }

    val hasInput: Boolean get() = visibleFields.any { field(it).text.isNotEmpty() }

    val hasResult: Boolean get() = result != null || ktaResult != null

    /** The ball requirement of a successful calculation, whatever the standard. */
    val requirement: BallRequirement?
        get() = (result as? CalculationResult.Success) ?: (ktaResult as? KtaResult.Success)

    /** The tube of a successful calculation, whatever the standard. */
    val calculatedTube: Tube?
        get() = (result as? CalculationResult.Success)?.dimensions ?: (ktaResult as? KtaResult.Success)?.tube

    /** True when the result needs a caution: an extrapolated table row or a tube outside the KTA scope. */
    val isOutsideStandard: Boolean
        get() = (result as? CalculationResult.Success)?.isExtrapolated == true ||
            (ktaResult as? KtaResult.Success)?.isOutsideScope == true
}

class CalculatorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: UserDataRepository,
    private val historyDelayMillis: Long = HISTORY_DELAY_MILLIS,
) : ViewModel() {

    private val inputs = MutableStateFlow(
        DimensionField.entries.associateWith { savedStateHandle.get<String>(it.name).orEmpty() },
    )

    private val restoredStandard = savedStateHandle.get<String>(STANDARD_KEY)
        ?.let { name -> Standard.entries.firstOrNull { it.name == name } }

    private val standard = MutableStateFlow(restoredStandard ?: Standard.VGB_S_013)

    val uiState: StateFlow<CalculatorUiState> =
        combine(inputs, standard, repository.ballSets, repository.history) { inputs, standard, ballSets, history ->
            buildState(inputs, standard, ballSets.active, history)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            buildState(inputs.value, standard.value, EMPTY_SET, emptyList()),
        )

    private var historyJob: Job? = null
    private var standardChosen = restoredStandard != null

    init {
        // A fresh start uses the standard chosen last time, unless the user picks one first.
        if (!standardChosen) {
            viewModelScope.launch {
                val stored = repository.standard.first()
                if (!standardChosen) standard.value = stored
            }
        }
    }

    fun onValueChange(field: DimensionField, text: String) {
        setInputs(inputs.value + (field to DecimalInput.sanitize(text)))
    }

    fun selectStandard(selected: Standard) {
        setStandard(selected)
        scheduleHistoryEntry()
    }

    /** Clears the fields of the current standard. */
    fun clearAll() {
        setInputs(inputs.value + standard.value.fields.associateWith { "" })
    }

    /** Fills the fields from a history entry, using [decimalSeparator] of the current locale. */
    fun applyHistory(tube: Tube, decimalSeparator: Char) {
        val third = when (tube) {
            is TubeDimensions -> DimensionField.BEND_RADIUS to tube.bendRadius
            is KtaTube -> DimensionField.UNDERSIZE to tube.undersize
        }
        setStandard(tube.standard)
        setInputs(
            inputs.value + mapOf(
                DimensionField.OUTER_DIAMETER to tube.outerDiameter,
                DimensionField.WALL_THICKNESS to tube.wallThickness,
                third,
            ).mapValues { (_, value) -> DecimalInput.format(value, decimalSeparator) },
        )
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    private fun setStandard(selected: Standard) {
        standardChosen = true
        savedStateHandle[STANDARD_KEY] = selected.name
        if (standard.value == selected) return
        standard.value = selected
        viewModelScope.launch { repository.setStandard(selected) }
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
        val tube = calculate(inputs.value, standard.value).calculatedTube ?: return
        historyJob = viewModelScope.launch {
            delay(historyDelayMillis)
            repository.addToHistory(tube)
        }
    }

    companion object {
        const val HISTORY_DELAY_MILLIS = 2_000L
        private const val STANDARD_KEY = "standard"

        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as BallPassageApplication
                CalculatorViewModel(createSavedStateHandle(), application.userDataRepository)
            }
        }

        private val EMPTY_SET = BallSet(id = "", name = "")

        internal fun buildState(
            inputs: Map<DimensionField, String>,
            standard: Standard,
            ballSet: BallSet,
            history: List<Tube>,
        ): CalculatorUiState {
            val calculated = calculate(inputs, standard)
            return calculated.copy(
                fittingBalls = calculated.requirement?.let { BallSelector.fittingBalls(it, ballSet.balls) }.orEmpty(),
                hasBallSet = ballSet.balls.isNotEmpty(),
                ballSetName = ballSet.name,
                history = history,
            )
        }

        private fun calculate(inputs: Map<DimensionField, String>, standard: Standard): CalculatorUiState {
            val values = mutableMapOf<DimensionField, BigDecimal>()
            val errors = mutableMapOf<DimensionField, FieldError>()

            for (field in standard.fields) {
                val text = inputs[field].orEmpty()
                if (text.isBlank()) continue
                val value = DecimalInput.parse(text)
                when {
                    value == null -> errors[field] = FieldError.INVALID_NUMBER
                    !field.allowsZero && value.signum() == 0 -> errors[field] = FieldError.MUST_BE_POSITIVE
                    else -> values[field] = value
                }
            }

            var result: CalculationResult? = null
            var ktaResult: KtaResult? = null
            if (errors.isEmpty() && values.size == standard.fields.size) {
                val outer = values.getValue(DimensionField.OUTER_DIAMETER)
                val wall = values.getValue(DimensionField.WALL_THICKNESS)
                when (standard) {
                    Standard.VGB_S_013 -> {
                        val calculated = BallPassageCalculator.calculate(
                            TubeDimensions(outer, wall, values.getValue(DimensionField.BEND_RADIUS)),
                        )
                        if (calculated is CalculationResult.Failure &&
                            CalculationProblem.WALL_TOO_THICK in calculated.problems
                        ) {
                            errors[DimensionField.WALL_THICKNESS] = FieldError.WALL_TOO_THICK
                        } else {
                            result = calculated
                        }
                    }
                    Standard.KTA_3211_1 -> {
                        val calculated = KtaCalculator.calculate(KtaTube(outer, wall, values.getValue(DimensionField.UNDERSIZE)))
                        if (calculated is KtaResult.Failure && calculated.problem == KtaProblem.WALL_TOO_THICK) {
                            errors[DimensionField.WALL_THICKNESS] = FieldError.WALL_TOO_THICK
                        } else {
                            ktaResult = calculated
                        }
                    }
                }
            }

            return CalculatorUiState(
                standard = standard,
                fields = DimensionField.entries.associateWith { FieldState(inputs[it].orEmpty(), errors[it]) },
                result = result,
                ktaResult = ktaResult,
            )
        }
    }
}
