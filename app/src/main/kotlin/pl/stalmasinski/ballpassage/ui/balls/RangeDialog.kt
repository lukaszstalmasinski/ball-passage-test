package pl.stalmasinski.ballpassage.ui.balls

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.common.DecimalInput

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
