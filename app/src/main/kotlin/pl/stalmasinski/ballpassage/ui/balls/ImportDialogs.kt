package pl.stalmasinski.ballpassage.ui.balls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.balls.SharedBallSet
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.common.rememberNumberFormatter

@Composable
fun ImportPasteDialog(onParse: (String) -> SharedBallSet?, onFound: (SharedBallSet) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var notFound by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_paste_title)) },
        text = {
            // Scrolls on small or landscape screens, so that the buttons stay reachable.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.import_paste_description), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        notFound = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.IMPORT_TEXT),
                    minLines = 4,
                    maxLines = 8,
                    isError = notFound,
                    supportingText = if (notFound) {
                        { Text(stringResource(R.string.import_not_found)) }
                    } else {
                        null
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onParse(text)?.let(onFound) ?: run { notFound = true } },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag(TestTags.DIALOG_CONFIRM),
            ) { Text(stringResource(R.string.import_next)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

@Composable
fun ImportConfirmDialog(shared: SharedBallSet, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val formatter = rememberNumberFormatter()
    val name = shared.name.ifEmpty { stringResource(R.string.set_default_name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_confirm_title, name)) },
        text = {
            // Scrolls on small or landscape screens, so that the buttons stay reachable.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(pluralStringResource(R.plurals.ball_count, shared.balls.size, shared.balls.size))
                Text(
                    text = shared.balls.joinToString { formatter.format(it.diameter) } + " " + stringResource(R.string.unit_mm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.import_confirm_note), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(TestTags.DIALOG_CONFIRM)) {
                Text(stringResource(R.string.import_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}
