package pl.stalmasinski.ballpassage.ui.calculator

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.ui.theme.BallPassageTheme

@Composable
fun CalculatorRoute(viewModel: CalculatorViewModel = viewModel(factory = CalculatorViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CalculatorScreen(state = state, onValueChange = viewModel::onValueChange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    onValueChange: (DimensionField, String) -> Unit,
) {
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showAbout = true }, modifier = Modifier.testTag(TestTags.ABOUT_BUTTON)) {
                        Icon(painterResource(R.drawable.ic_info), contentDescription = stringResource(R.string.about_open))
                    }
                },
                scrollBehavior = scrollBehavior,
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
                DimensionInputs(state = state, onValueChange = onValueChange)
                ResultCard(state = state)
                state.result?.let { CalculationBreakdown(result = it) }
                StandardTableCard(result = state.result)
            }
        }
    }

    if (showAbout) {
        AboutSheet(onDismiss = { showAbout = false })
    }
}

@Composable
private fun DimensionInputs(
    state: CalculatorUiState,
    onValueChange: (DimensionField, String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DimensionField.entries.forEach { field ->
            val fieldState = state.field(field)
            val label = stringResource(field.labelRes)
            val isLast = field == DimensionField.entries.last()

            OutlinedTextField(
                value = fieldState.text,
                onValueChange = { onValueChange(field, it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.field(field)),
                label = { Text(label) },
                suffix = { Text(stringResource(R.string.unit_mm)) },
                supportingText = {
                    Text(stringResource(fieldState.error?.messageRes ?: field.hintRes))
                },
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

@get:StringRes
private val DimensionField.hintRes: Int
    get() = when (this) {
        DimensionField.OUTER_DIAMETER -> R.string.field_outer_diameter_hint
        DimensionField.WALL_THICKNESS -> R.string.field_wall_thickness_hint
        DimensionField.BEND_RADIUS -> R.string.field_bend_radius_hint
    }

@get:StringRes
private val FieldError.messageRes: Int
    get() = when (this) {
        FieldError.INVALID_NUMBER -> R.string.error_invalid_number
        FieldError.MUST_BE_POSITIVE -> R.string.error_must_be_positive
        FieldError.WALL_TOO_THICK -> R.string.error_wall_too_thick
    }

object TestTags {
    const val ABOUT_BUTTON = "about_button"
    const val ABOUT_SHEET = "about_sheet"
    const val BALL_DIAMETER = "ball_diameter"
    const val EXTRAPOLATION_WARNING = "extrapolation_warning"
    const val FAILURE = "failure"

    fun field(field: DimensionField) = "field_${field.name}"

    fun tableCell(rowIndex: Int, columnIndex: Int) = "cell_${rowIndex}_$columnIndex"
}

@Preview(showBackground = true)
@Composable
private fun CalculatorScreenPreview() {
    BallPassageTheme(dynamicColor = false) {
        CalculatorScreen(
            state = CalculatorViewModel.buildState(
                mapOf(
                    DimensionField.OUTER_DIAMETER to "48.3",
                    DimensionField.WALL_THICKNESS to "5",
                    DimensionField.BEND_RADIUS to "70",
                ),
            ),
            onValueChange = { _, _ -> },
        )
    }
}
