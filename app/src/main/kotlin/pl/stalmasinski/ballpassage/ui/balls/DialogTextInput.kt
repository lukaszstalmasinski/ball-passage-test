package pl.stalmasinski.ballpassage.ui.balls

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import pl.stalmasinski.ballpassage.core.BallSets

@Composable
internal fun TextInput(
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
