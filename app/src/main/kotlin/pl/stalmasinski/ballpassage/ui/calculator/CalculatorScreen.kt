package pl.stalmasinski.ballpassage.ui.calculator

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.TubeDimensions
import pl.stalmasinski.ballpassage.ui.AppTopBar
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.rememberAppTopBarScrollBehavior
import pl.stalmasinski.ballpassage.ui.theme.BallPassageTheme
import java.math.BigDecimal
import java.text.DecimalFormatSymbols

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    onValueChange: (DimensionField, String) -> Unit,
    onClearAll: () -> Unit,
    onApplyHistory: (TubeDimensions, Char) -> Unit,
    onClearHistory: () -> Unit,
    onOpenBalls: () -> Unit,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                scrollBehavior = scrollBehavior,
                actions = {
                    if (state.hasInput) {
                        IconButton(onClick = onClearAll, modifier = Modifier.testTag(TestTags.CLEAR_ALL)) {
                            Icon(painterResource(R.drawable.ic_clear_all), contentDescription = stringResource(R.string.clear_all))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.history.isNotEmpty()) {
                    RecentTubes(history = state.history, onApply = onApplyHistory, onClear = onClearHistory)
                }
                DimensionInputs(state = state, onValueChange = onValueChange)
                ResultCard(state = state, onOpenBalls = onOpenBalls)
                state.result?.let { CalculationBreakdown(result = it) }
                StandardTableCard(result = state.result)
            }
        }
    }
}

@Composable
private fun RecentTubes(
    history: List<TubeDimensions>,
    onApply: (TubeDimensions, Char) -> Unit,
    onClear: () -> Unit,
) {
    val formatter = rememberNumberFormatter()
    val locale = LocalConfiguration.current.locales[0]
    val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator

    // A single row keeps the inputs and the result high on small screens.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_history),
            contentDescription = stringResource(R.string.recent_title),
            modifier = Modifier
                .padding(end = 8.dp)
                .size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            history.forEachIndexed { index, tube ->
                SuggestionChip(
                    onClick = { onApply(tube, decimalSeparator) },
                    label = {
                        Text(
                            stringResource(
                                R.string.recent_item,
                                formatter.format(tube.outerDiameter),
                                formatter.format(tube.wallThickness),
                                formatter.format(tube.bendRadius),
                            ),
                        )
                    },
                    modifier = Modifier.testTag(TestTags.historyItem(index)),
                )
            }
        }
        IconButton(onClick = onClear, modifier = Modifier.testTag(TestTags.CLEAR_HISTORY)) {
            Icon(
                painterResource(R.drawable.ic_clear),
                contentDescription = stringResource(R.string.recent_clear),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DimensionInputs(
    state: CalculatorUiState,
    onValueChange: (DimensionField, String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DimensionField.entries.forEach { field ->
            val fieldState = state.field(field)
            val label = stringResource(field.labelRes)
            val isLast = field == DimensionField.entries.last()
            val supportingText = fieldState.error?.messageRes ?: field.hintRes

            OutlinedTextField(
                value = fieldState.text,
                onValueChange = { onValueChange(field, it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.field(field)),
                label = { Text(label) },
                suffix = { Text(stringResource(R.string.unit_mm)) },
                supportingText = supportingText?.let { { Text(stringResource(it)) } },
                isError = fieldState.error != null,
                trailingIcon = if (fieldState.text.isNotEmpty()) {
                    {
                        IconButton(onClick = { onValueChange(field, "") }) {
                            Icon(
                                painterResource(R.drawable.ic_clear),
                                contentDescription = stringResource(R.string.clear_field, label),
                            )
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
        }
    }
}

@get:StringRes
private val DimensionField.labelRes: Int
    get() = when (this) {
        DimensionField.OUTER_DIAMETER -> R.string.field_outer_diameter
        DimensionField.WALL_THICKNESS -> R.string.field_wall_thickness
        DimensionField.BEND_RADIUS -> R.string.field_bend_radius
    }

/** Only the bend radius needs an explanation; the other labels are self-explanatory. */
@get:StringRes
private val DimensionField.hintRes: Int?
    get() = when (this) {
        DimensionField.BEND_RADIUS -> R.string.field_bend_radius_hint
        else -> null
    }

@get:StringRes
private val FieldError.messageRes: Int
    get() = when (this) {
        FieldError.INVALID_NUMBER -> R.string.error_invalid_number
        FieldError.MUST_BE_POSITIVE -> R.string.error_must_be_positive
        FieldError.WALL_TOO_THICK -> R.string.error_wall_too_thick
    }

@Preview(showBackground = true)
@Composable
private fun CalculatorScreenPreview() {
    BallPassageTheme(dynamicColor = false) {
        CalculatorScreen(
            state = CalculatorViewModel.buildState(
                inputs = mapOf(
                    DimensionField.OUTER_DIAMETER to "48.3",
                    DimensionField.WALL_THICKNESS to "5",
                    DimensionField.BEND_RADIUS to "70",
                ),
                balls = listOf(BigDecimal("32"), BigDecimal("35")),
                history = listOf(TubeDimensions(BigDecimal("33.7"), BigDecimal("4.5"), BigDecimal("60"))),
            ),
            onValueChange = { _, _ -> },
            onClearAll = {},
            onApplyHistory = { _, _ -> },
            onClearHistory = {},
            onOpenBalls = {},
        )
    }
}
