package pl.stalmasinski.ballpassage.ui.balls

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.Ball
import pl.stalmasinski.ballpassage.core.BallSets
import pl.stalmasinski.ballpassage.core.SharedBallSet
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.calculator.DecimalInput
import pl.stalmasinski.ballpassage.ui.calculator.rememberNumberFormatter
import pl.stalmasinski.ballpassage.ui.qr.QrCodeImage
import pl.stalmasinski.ballpassage.ui.qr.QrCodes

@Composable
fun RangeDialog(onAdd: (from: String, to: String, step: String) -> RangeResult, onDismiss: () -> Unit) {
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    var step by rememberSaveable { mutableStateOf(BallsViewModel.DEFAULT_RANGE_STEP.toPlainString()) }
    var error by rememberSaveable { mutableStateOf<RangeError?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.range_title)) },
        text = {
            // Scrolls on small or landscape screens, so that the buttons stay reachable.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.range_description), style = MaterialTheme.typography.bodyMedium)
                listOf(
                    Triple(R.string.range_from, from) { value: String -> from = value },
                    Triple(R.string.range_to, to) { value: String -> to = value },
                    Triple(R.string.range_step, step) { value: String -> step = value },
                ).forEachIndexed { index, (label, value, onChange) ->
                    NumberField(
                        label = label,
                        value = value,
                        onValueChange = {
                            onChange(DecimalInput.sanitize(it))
                            error = null
                        },
                        isLast = index == 2,
                        modifier = Modifier.testTag(TestTags.rangeField(index)),
                    )
                }
                error?.let { Text(stringResource(it.messageRes), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (val result = onAdd(from, to, step)) {
                        is RangeResult.Added -> onDismiss()
                        is RangeResult.Invalid -> error = result.error
                    }
                },
                modifier = Modifier.testTag(TestTags.RANGE_CONFIRM),
            ) { Text(stringResource(R.string.range_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

@Composable
private fun NumberField(
    @StringRes label: Int,
    value: String,
    onValueChange: (String) -> Unit,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        suffix = { Text(stringResource(R.string.unit_mm)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
        ),
    )
}

@get:StringRes
private val RangeError.messageRes: Int
    get() = when (this) {
        RangeError.INVALID_NUMBER -> R.string.range_error_invalid
        RangeError.FROM_NOT_POSITIVE -> R.string.range_error_from
        RangeError.STEP_NOT_POSITIVE -> R.string.range_error_step
        RangeError.TO_BELOW_FROM -> R.string.range_error_order
        RangeError.TOO_MANY -> R.string.range_error_too_many
    }

@Composable
fun BallDetailsDialog(ball: Ball, onSave: (label: String, location: String) -> Unit, onDismiss: () -> Unit) {
    val formatter = rememberNumberFormatter()
    var label by rememberSaveable { mutableStateOf(ball.label) }
    var location by rememberSaveable { mutableStateOf(ball.location) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ball_size, formatter.format(ball.diameter))) },
        text = {
            // Scrolls on small or landscape screens, so that the buttons stay reachable.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextInput(R.string.ball_label, R.string.ball_label_hint, label, { label = it }, TestTags.BALL_LABEL)
                TextInput(R.string.ball_location, R.string.ball_location_hint, location, { location = it }, TestTags.BALL_LOCATION)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(label, location) }, modifier = Modifier.testTag(TestTags.DIALOG_CONFIRM)) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

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

@Composable
private fun TextInput(
    @StringRes label: Int,
    @StringRes placeholder: Int,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.replace('\n', ' ').take(BallSets.MAX_TEXT_LENGTH)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        label = { Text(stringResource(label)) },
        placeholder = { Text(stringResource(placeholder)) },
        singleLine = true,
    )
}

/** QR code to scan on another phone, plus sharing as text for messages and e-mail. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(setName: String, ballCount: Int, payload: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.export_share_text)
    val message = stringResource(R.string.export_message_header, setName) + "\n\n" + payload + "\n\n" +
        stringResource(R.string.export_message_footer)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .testTag(TestTags.EXPORT_SHEET)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.export_title, setName), style = MaterialTheme.typography.titleLarge)
            Text(
                text = pluralStringResource(R.plurals.ball_count, ballCount, ballCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (QrCodes.fits(payload)) {
                QrCodeImage(text = payload, contentDescription = stringResource(R.string.export_qr_description), size = 260.dp)
                Text(
                    text = stringResource(R.string.export_qr_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = stringResource(R.string.export_too_large),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Button(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                    }
                    context.startActivity(Intent.createChooser(send, shareTitle))
                },
                modifier = Modifier.testTag(TestTags.EXPORT_SHARE_TEXT),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_share), contentDescription = null)
                    Text(shareTitle)
                }
            }
        }
    }
}

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
