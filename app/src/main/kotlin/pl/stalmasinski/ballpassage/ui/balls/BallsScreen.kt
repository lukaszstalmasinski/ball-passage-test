package pl.stalmasinski.ballpassage.ui.balls

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import pl.stalmasinski.ballpassage.core.Ball
import pl.stalmasinski.ballpassage.core.BallSet
import pl.stalmasinski.ballpassage.core.CalculationResult
import pl.stalmasinski.ballpassage.core.SharedBallSet
import pl.stalmasinski.ballpassage.ui.AppTopBar
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.calculator.rememberNumberFormatter
import pl.stalmasinski.ballpassage.ui.qr.QrScannerDialog
import pl.stalmasinski.ballpassage.ui.rememberAppTopBarScrollBehavior

/** The calculator's current tube, so that the list can mark the balls that fit it. */
data class TubeContext(
    val result: CalculationResult.Success,
    val fittingBalls: List<Ball>,
)

@Composable
fun BallsRoute(
    tubeContext: TubeContext?,
    viewModel: BallsViewModel = viewModel(factory = BallsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BallsScreen(state = state, tubeContext = tubeContext, actions = viewModel)
}

private enum class BallsDialog { RANGE, NEW_SET, RENAME_SET, DELETE_SET, EXPORT, IMPORT_PASTE, IMPORT_SCAN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BallsScreen(state: BallsUiState, tubeContext: TubeContext?, actions: BallsViewModel) {
    val formatter = rememberNumberFormatter()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    val removedMessage = stringResource(R.string.balls_removed)
    val undoLabel = stringResource(R.string.balls_undo)
    val notFoundMessage = stringResource(R.string.import_not_found)
    val defaultSetName = stringResource(R.string.set_default_name)
    val importedSetName = stringResource(R.string.set_imported_name)
    val activeSetName = state.activeSet.name.ifEmpty { defaultSetName }

    var dialog by rememberSaveable { mutableStateOf<BallsDialog?>(null) }
    var editedBallId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<SharedBallSet?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.balls_title),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        onClick = { dialog = BallsDialog.EXPORT },
                        enabled = state.balls.isNotEmpty(),
                        modifier = Modifier.testTag(TestTags.EXPORT),
                    ) {
                        Icon(painterResource(R.drawable.ic_share), contentDescription = stringResource(R.string.export_action))
                    }
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.testTag(TestTags.BALLS_MENU)) {
                        Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.more_options))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        MenuItem(R.string.import_scan, R.drawable.ic_qr_scan) {
                            menuExpanded = false
                            dialog = BallsDialog.IMPORT_SCAN
                        }
                        MenuItem(R.string.import_paste, R.drawable.ic_paste) {
                            menuExpanded = false
                            dialog = BallsDialog.IMPORT_PASTE
                        }
                        HorizontalDivider()
                        MenuItem(R.string.set_rename, R.drawable.ic_edit) {
                            menuExpanded = false
                            dialog = BallsDialog.RENAME_SET
                        }
                        MenuItem(R.string.set_delete, R.drawable.ic_delete, enabled = state.canDeleteSet) {
                            menuExpanded = false
                            dialog = BallsDialog.DELETE_SET
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        // One scrolling list, like the other tabs, so that the top bar tints consistently.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val itemModifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()

            item {
                SetSelector(
                    sets = state.sets,
                    active = state.activeSet,
                    defaultName = defaultSetName,
                    onSelect = actions::selectSet,
                    onNewSet = { dialog = BallsDialog.NEW_SET },
                    modifier = itemModifier,
                )
            }
            item { TubeContextCard(tubeContext, hasBalls = state.balls.isNotEmpty(), modifier = itemModifier) }
            item {
                AddBallRow(
                    state = state,
                    onInputChange = actions::onInputChange,
                    onAdd = { actions.add() },
                    onAddRange = { dialog = BallsDialog.RANGE },
                    modifier = itemModifier,
                )
            }
            item {
                BallList(
                    balls = state.balls,
                    tubeContext = tubeContext,
                    onEdit = { editedBallId = it.id },
                    onRemove = { ball ->
                        actions.remove(ball)
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = removedMessage.format(formatter.format(ball.diameter)),
                                actionLabel = undoLabel,
                                duration = SnackbarDuration.Long,
                            )
                            if (result == SnackbarResult.ActionPerformed) actions.restore(ball)
                        }
                    },
                    modifier = itemModifier,
                )
            }
        }
    }

    val editedBall = state.balls.firstOrNull { it.id == editedBallId }
    if (editedBall != null) {
        BallDetailsDialog(
            ball = editedBall,
            onSave = { label, location ->
                actions.updateDetails(editedBall, label, location)
                editedBallId = null
            },
            onDismiss = { editedBallId = null },
        )
    }

    val close = { dialog = null }
    when (dialog) {
        BallsDialog.RANGE -> RangeDialog(onAdd = actions::addRange, onDismiss = close)
        BallsDialog.NEW_SET -> SetNameDialog(
            title = R.string.set_new_title,
            initialName = "",
            onConfirm = {
                actions.createSet(it)
                close()
            },
            onDismiss = close,
        )
        BallsDialog.RENAME_SET -> SetNameDialog(
            title = R.string.set_rename_title,
            initialName = activeSetName,
            onConfirm = {
                actions.renameSet(it)
                close()
            },
            onDismiss = close,
        )
        BallsDialog.DELETE_SET -> DeleteSetDialog(
            setName = activeSetName,
            ballCount = state.balls.size,
            onConfirm = {
                actions.deleteSet()
                close()
            },
            onDismiss = close,
        )
        BallsDialog.EXPORT -> ExportSheet(activeSetName, state.balls.size, actions.exportText(activeSetName), close)
        BallsDialog.IMPORT_PASTE -> ImportPasteDialog(
            onParse = actions::parseImport,
            onFound = {
                pendingImport = it
                close()
            },
            onDismiss = close,
        )
        BallsDialog.IMPORT_SCAN -> QrScannerDialog(
            onResult = { text ->
                close()
                val shared = actions.parseImport(text)
                if (shared != null) {
                    pendingImport = shared
                } else {
                    scope.launch { snackbarHostState.showSnackbar(notFoundMessage) }
                }
            },
            onDismiss = close,
        )
        null -> Unit
    }

    pendingImport?.let { shared ->
        ImportConfirmDialog(
            shared = shared,
            onConfirm = {
                actions.importSet(shared.copy(name = shared.name.ifEmpty { importedSetName }))
                pendingImport = null
            },
            onDismiss = { pendingImport = null },
        )
    }
}

@Composable
private fun MenuItem(@StringRes text: Int, icon: Int, enabled: Boolean = true, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(text)) },
        onClick = onClick,
        enabled = enabled,
        leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetSelector(
    sets: List<BallSet>,
    active: BallSet,
    defaultName: String,
    onSelect: (BallSet) -> Unit,
    onNewSet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = active.name.ifEmpty { defaultName },
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.set_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .testTag(TestTags.SET_SELECTOR),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sets.forEach { set ->
                DropdownMenuItem(
                    text = { Text(set.name.ifEmpty { defaultName }) },
                    onClick = {
                        onSelect(set)
                        expanded = false
                    },
                    trailingIcon = if (set.id == active.id) {
                        { Icon(painterResource(R.drawable.ic_check), contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.set_new)) },
                onClick = {
                    expanded = false
                    onNewSet()
                },
                leadingIcon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                modifier = Modifier.testTag(TestTags.SET_NEW),
            )
        }
    }
}

/** Tells which tube from the calculator the marks in the list refer to. */
@Composable
private fun TubeContextCard(tubeContext: TubeContext?, hasBalls: Boolean, modifier: Modifier = Modifier) {
    if (tubeContext == null && !hasBalls) return
    val formatter = rememberNumberFormatter()

    Card(
        modifier = modifier.testTag(TestTags.TUBE_CONTEXT),
        colors = CardDefaults.cardColors(
            containerColor = if (tubeContext != null) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (tubeContext == null) {
                Text(
                    text = stringResource(R.string.context_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            val dimensions = tubeContext.result.dimensions
            Text(
                text = stringResource(
                    R.string.context_tube,
                    formatter.format(dimensions.outerDiameter),
                    formatter.format(dimensions.wallThickness),
                    formatter.format(dimensions.bendRadius),
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    R.string.result_range,
                    formatter.format(tubeContext.result.minBallDiameter),
                    formatter.format(tubeContext.result.maxBallDiameterExclusive),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (hasBalls && tubeContext.fittingBalls.isEmpty()) {
                Text(stringResource(R.string.ball_none_fits), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun AddBallRow(
    state: BallsUiState,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onAddRange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.BALL_INPUT),
                label = { Text(stringResource(R.string.balls_input_label)) },
                suffix = { Text(stringResource(R.string.unit_mm)) },
                supportingText = state.inputError?.let { error -> { Text(stringResource(error.messageRes)) } },
                isError = state.inputError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAdd() }),
            )
            FilledIconButton(
                onClick = onAdd,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(48.dp)
                    .testTag(TestTags.BALL_ADD),
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.balls_add))
            }
        }
        TextButton(onClick = onAddRange, modifier = Modifier.testTag(TestTags.RANGE_OPEN)) {
            Text(stringResource(R.string.range_open))
        }
    }
}

@Composable
private fun BallList(
    balls: List<Ball>,
    tubeContext: TubeContext?,
    onEdit: (Ball) -> Unit,
    onRemove: (Ball) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = rememberNumberFormatter()
    val recommendedId = tubeContext?.fittingBalls?.firstOrNull()?.id
    val fittingIds = tubeContext?.fittingBalls.orEmpty().map { it.id }.toSet()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        if (balls.isEmpty()) {
            Text(
                text = stringResource(R.string.balls_empty),
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Card
        }
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            // One hint for the whole list instead of repeating it in every row.
            Text(
                text = stringResource(R.string.ball_add_details),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            balls.forEachIndexed { index, ball ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                val size = formatter.format(ball.diameter)
                val details = ball.details()
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.ball_size, size), style = MaterialTheme.typography.titleMedium)
                    },
                    supportingContent = if (details.isNotEmpty()) {
                        { Text(details) }
                    } else {
                        null
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (ball.id) {
                                recommendedId -> FitBadge(R.string.ball_badge_recommended, recommended = true)
                                in fittingIds -> FitBadge(R.string.ball_badge_fits, recommended = false)
                            }
                            IconButton(onClick = { onRemove(ball) }) {
                                Icon(
                                    painterResource(R.drawable.ic_delete),
                                    contentDescription = stringResource(R.string.balls_remove, size),
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .clickable { onEdit(ball) }
                        .padding(start = 4.dp)
                        .testTag(TestTags.ballItem(ball.diameter)),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun FitBadge(@StringRes text: Int, recommended: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (recommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (recommended) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = stringResource(text),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Label and location joined for display, e.g. "K-12 · case 2". */
fun Ball.details(): String = listOf(label, location).filter { it.isNotEmpty() }.joinToString(" · ")

@get:StringRes
private val BallInputError.messageRes: Int
    get() = when (this) {
        BallInputError.INVALID_NUMBER -> R.string.error_invalid_number
        BallInputError.MUST_BE_POSITIVE -> R.string.error_must_be_positive
        BallInputError.DUPLICATE -> R.string.balls_error_duplicate
    }
