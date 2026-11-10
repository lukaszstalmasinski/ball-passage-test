package pl.stalmasinski.ballpassage.ui.balls

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.Ball
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.common.details
import pl.stalmasinski.ballpassage.ui.common.rememberNumberFormatter

@Composable
internal fun BallList(
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
