package pl.stalmasinski.ballpassage.ui.balls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.Ball
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.common.rememberNumberFormatter

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
