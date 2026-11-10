package pl.stalmasinski.ballpassage.ui.balls

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.balls.SharedBallSet
import pl.stalmasinski.ballpassage.ui.AppTopBar
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.common.rememberNumberFormatter
import pl.stalmasinski.ballpassage.ui.qr.QrScannerDialog
import pl.stalmasinski.ballpassage.ui.rememberAppTopBarScrollBehavior

@Composable
fun BallsRoute(
    tubeContext: TubeContext?,
    viewModel: BallsViewModel = viewModel(factory = BallsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) {
        onDispose { viewModel.onScreenLeft() }
    }
    BallsScreen(state = state, tubeContext = tubeContext, actions = viewModel)
}

private enum class BallsDialog { RANGE, NEW_SET, RENAME_SET, DELETE_SET, EXPORT, IMPORT_PASTE, IMPORT_SCAN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BallsScreen(state: BallsUiState, tubeContext: TubeContext?, actions: BallsActions) {
    val formatter = rememberNumberFormatter()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    val removedMessage = stringResource(R.string.balls_removed)
    val undoLabel = stringResource(R.string.balls_undo)
    val notFoundMessage = stringResource(R.string.import_not_found)
    val saveFailedMessage = stringResource(R.string.balls_save_failed)

    LaunchedEffect(state.saveFailed) {
        if (state.saveFailed) {
            snackbarHostState.showSnackbar(saveFailedMessage, duration = SnackbarDuration.Long)
            actions.onSaveErrorShown()
        }
    }
    val defaultSetName = stringResource(R.string.set_default_name)
    val importedSetName = stringResource(R.string.set_imported_name)
    val activeSetName = state.activeSet.name.ifEmpty { defaultSetName }

    var dialog by rememberSaveable { mutableStateOf<BallsDialog?>(null) }
    var editedBallId by rememberSaveable { mutableStateOf<String?>(null) }
    // A scanned or pasted set waits here for confirmation; it survives rotation.
    var pendingImport by rememberSaveable(stateSaver = SharedBallSetSaver) { mutableStateOf<SharedBallSet?>(null) }
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val itemModifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
            // The balls are separate items, so the space between sections is part of each section.
            val sectionModifier = itemModifier.padding(bottom = 16.dp)

            item {
                SetSelector(
                    sets = state.sets,
                    active = state.activeSet,
                    defaultName = defaultSetName,
                    onSelect = actions::selectSet,
                    onNewSet = { dialog = BallsDialog.NEW_SET },
                    modifier = sectionModifier,
                )
            }
            item { TubeContextCard(tubeContext, hasBalls = state.balls.isNotEmpty(), modifier = sectionModifier) }
            item {
                AddBallRow(
                    state = state,
                    onInputChange = actions::onInputChange,
                    onAdd = { actions.add() },
                    onAddRange = { dialog = BallsDialog.RANGE },
                    modifier = sectionModifier,
                )
            }
            ballList(
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
