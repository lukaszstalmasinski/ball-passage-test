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
import pl.stalmasinski.ballpassage.core.Ball
import pl.stalmasinski.ballpassage.core.BallRange
import pl.stalmasinski.ballpassage.core.BallSet
import pl.stalmasinski.ballpassage.core.BallSetCodec
import pl.stalmasinski.ballpassage.core.BallSets
import pl.stalmasinski.ballpassage.core.SharedBallSet
import pl.stalmasinski.ballpassage.data.UserDataRepository
import pl.stalmasinski.ballpassage.ui.calculator.DecimalInput
import java.math.BigDecimal
import java.util.UUID

enum class BallInputError {
    INVALID_NUMBER,
    MUST_BE_POSITIVE,
    DUPLICATE,
}

sealed interface RangeResult {
    /** [count] new balls were added; diameters already in the set are skipped. */
    data class Added(val count: Int) : RangeResult

    data class Invalid(val error: RangeError) : RangeResult
}

enum class RangeError {
    INVALID_NUMBER,
    FROM_NOT_POSITIVE,
    STEP_NOT_POSITIVE,
    TO_BELOW_FROM,
    TOO_MANY,
}

data class BallsUiState(
    val sets: List<BallSet> = emptyList(),
    val activeSet: BallSet = BallSet(id = "", name = ""),
    val input: String = "",
    val inputError: BallInputError? = null,
) {
    val balls: List<Ball> get() = activeSet.balls

    val canDeleteSet: Boolean get() = sets.size > 1
}

class BallsViewModel(
    private val repository: UserDataRepository,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    private val input = MutableStateFlow("")
    private val inputError = MutableStateFlow<BallInputError?>(null)

    val uiState: StateFlow<BallsUiState> =
        combine(repository.ballSets, input, inputError) { sets, input, error ->
            BallsUiState(sets = sets.sets, activeSet = sets.active, input = input, inputError = error)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, BallsUiState())

    private val activeSetId: String get() = uiState.value.activeSet.id

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
            uiState.value.balls.any { it.diameter.compareTo(value) == 0 } -> BallInputError.DUPLICATE
            else -> null
        }
        if (value == null || inputError.value != null) return false

        update { it.addBalls(activeSetId, listOf(value), newId) }
        input.value = ""
        return true
    }

    fun addRange(from: String, to: String, step: String): RangeResult {
        val values = listOf(from, to, step).map(DecimalInput::parse)
        if (values.any { it == null }) return RangeResult.Invalid(RangeError.INVALID_NUMBER)

        return when (val range = BallRange.of(values[0]!!, values[1]!!, values[2]!!)) {
            is BallRange.Result.Invalid -> RangeResult.Invalid(range.error.toRangeError())
            is BallRange.Result.Valid -> {
                val existing = uiState.value.balls.map { it.diameter }
                val count = range.diameters.count { d -> existing.none { it.compareTo(d) == 0 } }
                update { it.addBalls(activeSetId, range.diameters, newId) }
                RangeResult.Added(count)
            }
        }
    }

    fun remove(ball: Ball) = update { it.removeBall(activeSetId, ball.id) }

    /** Puts back a ball removed by mistake, with its label and location. */
    fun restore(ball: Ball) = update { it.restoreBall(activeSetId, ball) }

    fun updateDetails(ball: Ball, label: String, location: String) =
        update { it.updateBallDetails(activeSetId, ball.id, label, location) }

    fun createSet(name: String) {
        val id = newId()
        update { it.createSet(name, id) }
    }

    fun renameSet(name: String) = update { it.renameSet(activeSetId, name) }

    fun deleteSet() = update { it.deleteSet(activeSetId) }

    fun selectSet(set: BallSet) = update { it.selectSet(set.id) }

    /** The active set in the sharing format, for a QR code or a message, under its displayed [name]. */
    fun exportText(name: String): String = BallSetCodec.encode(uiState.value.activeSet.copy(name = name))

    fun parseImport(text: String): SharedBallSet? = BallSetCodec.decode(text)

    fun importSet(shared: SharedBallSet) = update { it.importSet(shared, newId) }

    private fun update(transform: (BallSets) -> BallSets) {
        viewModelScope.launch { repository.updateBallSets(transform) }
    }

    private fun BallRange.Error.toRangeError() = when (this) {
        BallRange.Error.FROM_NOT_POSITIVE -> RangeError.FROM_NOT_POSITIVE
        BallRange.Error.STEP_NOT_POSITIVE -> RangeError.STEP_NOT_POSITIVE
        BallRange.Error.TO_BELOW_FROM -> RangeError.TO_BELOW_FROM
        BallRange.Error.TOO_MANY -> RangeError.TOO_MANY
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as BallPassageApplication
                BallsViewModel(application.userDataRepository)
            }
        }

        /** Shown in the range dialog as the default step. */
        val DEFAULT_RANGE_STEP: BigDecimal = BigDecimal.ONE
    }
}
