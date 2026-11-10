package pl.stalmasinski.ballpassage.ui.balls

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.ui.TestTags

/** Used for creating and renaming a set. */
@Composable
fun SetNameDialog(@StringRes title: Int, initialName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { TextInput(R.string.set_name, R.string.set_name_hint, name, { name = it }, TestTags.SET_NAME) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag(TestTags.DIALOG_CONFIRM),
            ) { Text(stringResource(R.string.dialog_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

@Composable
fun DeleteSetDialog(setName: String, ballCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_delete_title, setName)) },
        text = {
            Text(pluralStringResource(R.plurals.set_delete_message, ballCount, ballCount))
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(TestTags.DIALOG_CONFIRM)) {
                Text(stringResource(R.string.set_delete_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}
