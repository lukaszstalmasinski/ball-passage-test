package pl.stalmasinski.ballpassage.ui.balls

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.calculator.rememberNumberFormatter
import java.math.BigDecimal

@Composable
fun BallsRoute(viewModel: BallsViewModel = viewModel(factory = BallsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BallsScreen(
        state = state,
        onInputChange = viewModel::onInputChange,
        onAdd = viewModel::add,
        onRemove = viewModel::remove,
        onRestore = viewModel::restore,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BallsScreen(
    state: BallsUiState,
    onInputChange: (String) -> Unit,
    onAdd: () -> Boolean,
    onRemove: (BigDecimal) -> Unit,
    onRestore: (BigDecimal) -> Unit,
) {
    val formatter = rememberNumberFormatter()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val removedMessage = stringResource(R.string.balls_removed)
    val undoLabel = stringResource(R.string.balls_undo)

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.balls_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.balls_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.input,
                            onValueChange = onInputChange,
                            modifier = Modifier
                                .weight(1f)
                                .testTag(TestTags.BALL_INPUT),
                            label = { Text(stringResource(R.string.balls_input_label)) },
                            suffix = { Text(stringResource(R.string.unit_mm)) },
                            supportingText = state.inputError?.let { error ->
                                { Text(stringResource(error.messageRes)) }
                            },
                            isError = state.inputError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { onAdd() }),
                        )
                        FilledIconButton(
                            onClick = { onAdd() },
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .testTag(TestTags.BALL_ADD),
                        ) {
                            Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.balls_add))
                        }
                    }
                }

                if (state.balls.isEmpty()) {
                    Text(
                        text = stringResource(R.string.balls_empty),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                        items(state.balls, key = { it.toPlainString() }) { ball ->
                            val size = formatter.format(ball)
                            ListItem(
                                headlineContent = {
                                    Text(stringResource(R.string.ball_size, size), style = MaterialTheme.typography.titleMedium)
                                },
                                modifier = Modifier.testTag(TestTags.ballItem(ball)),
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            onRemove(ball)
                                            scope.launch {
                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                val result = snackbarHostState.showSnackbar(
                                                    message = removedMessage.format(size),
                                                    actionLabel = undoLabel,
                                                    duration = SnackbarDuration.Short,
                                                )
                                                if (result == SnackbarResult.ActionPerformed) onRestore(ball)
                                            }
                                        },
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.ic_delete),
                                            contentDescription = stringResource(R.string.balls_remove, size),
                                        )
                                    }
                                },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@get:StringRes
private val BallInputError.messageRes: Int
    get() = when (this) {
        BallInputError.INVALID_NUMBER -> R.string.error_invalid_number
        BallInputError.MUST_BE_POSITIVE -> R.string.error_must_be_positive
        BallInputError.DUPLICATE -> R.string.balls_error_duplicate
    }
