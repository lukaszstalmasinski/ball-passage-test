package pl.stalmasinski.ballpassage.ui.balls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.balls.BallSet
import pl.stalmasinski.ballpassage.ui.TestTags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SetSelector(
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
