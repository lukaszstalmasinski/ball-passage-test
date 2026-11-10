package pl.stalmasinski.ballpassage.ui.balls

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.ui.TestTags

@Composable
internal fun AddBallRow(
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
                // Centred on the field's 56 dp outline, which starts 8 dp down to leave room for the label.
                modifier = Modifier
                    .padding(top = 12.dp)
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

@get:StringRes
private val BallInputError.messageRes: Int
    get() = when (this) {
        BallInputError.INVALID_NUMBER -> R.string.error_invalid_number
        BallInputError.MUST_BE_POSITIVE -> R.string.error_must_be_positive
        BallInputError.DUPLICATE -> R.string.balls_error_duplicate
    }
